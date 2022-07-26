/*
 *
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  n compliance with the License.
 *  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.carbon.apimgt.impl.dao;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.impl.dao.constants.SQLConstants;
import org.wso2.carbon.apimgt.impl.dto.CorrelationConfigDTO;
import org.wso2.carbon.apimgt.impl.dto.CorrelationConfigPropertyDTO;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 * Database Access Library for the Correlation Configs Feature.
 */
public class CorrelationConfigDAO {
    private static final Log log = LogFactory.getLog(CorrelationConfigDAO.class);
    private static final CorrelationConfigDAO correlationConfigDAO = new CorrelationConfigDAO();

    private static final String COMPONENT_NAME = "COMPONENT_NAME";
    private static final String ENABLED = "ENABLED";
    private static final String PROPERTY_NAME = "PROPERTY_NAME";
    private static final String PROPERTY_VALUE = "PROPERTY_VALUE";
    private static final String DENIED_THREADS = "deniedThreads";
    private static final String[] DEFAULT_CORRELATION_COMPONENTS = {
            "http", "ldap", "synapse", "jdbc", "method-calls"};
    private static final String DEFAULT_DENIED_THREADS = "MessageDeliveryTaskThreadPool , HumanTaskServer , " +
            "BPFLServer, CarbonDeploymentSchedulerThread";

    private CorrelationConfigDAO() {

    }

    public static CorrelationConfigDAO getInstance() {
        return correlationConfigDAO;
    }

    public boolean updateCorrelationConfigs(List<CorrelationConfigDTO> correlationConfigDTOList)
            throws APIManagementException {
        String queryConfigs = SQLConstants.UPDATE_CORRELATION_CONFIGS;
        String queryProps = SQLConstants.UPDATE_CORRELATION_CONFIG_PROPERTIES;
        try (Connection connection = APIMgtDBUtil.getConnection()) {

            connection.setAutoCommit(false);
            log.debug("Updating Correlation Configs");
            try (PreparedStatement preparedStatementConfigs = connection.prepareStatement(queryConfigs);
                    PreparedStatement preparedStatementProps = connection.prepareStatement(queryProps)) {
                for (CorrelationConfigDTO correlationConfigDTO : correlationConfigDTOList) {
                    String componentName = correlationConfigDTO.getName();
                    String enabled = correlationConfigDTO.getEnabled();

                    preparedStatementConfigs.setString(1, enabled);
                    preparedStatementConfigs.setString(2, componentName);
                    preparedStatementConfigs.addBatch();

                    List<CorrelationConfigPropertyDTO> correlationConfigPropertyDTOList =
                            correlationConfigDTO.getProperties();
                    if (correlationConfigPropertyDTOList == null) {
                        continue;
                    }

                    for (CorrelationConfigPropertyDTO correlationConfigPropertyDTO : correlationConfigPropertyDTOList) {
                        String propertyName = correlationConfigPropertyDTO.getName();
                        String propertyValue = String.join(",", correlationConfigPropertyDTO.getValue());

                        if (!propertyName.equals(DENIED_THREADS) || !componentName.equals("jdbc")) {
                            throw new APIManagementException(
                                    componentName + " does not have a \"" + propertyName + "\" property");
                        }


                        preparedStatementProps.setString(1, propertyValue);
                        preparedStatementProps.setString(2, componentName);
                        preparedStatementProps.setString(3, propertyName);
                        preparedStatementProps.addBatch();
                    }
                    preparedStatementProps.executeBatch();

                }
                preparedStatementConfigs.executeBatch();
                connection.commit();
                return true;
            } catch (APIManagementException e) {
                connection.rollback();
                throw new APIManagementException(e.getMessage(),
                        ExceptionCodes.from(ExceptionCodes.CORRELATION_CONFIG_PROPERTY_NOT_SUPPORTED));
            }

        } catch (SQLException e) {
            log.error("Failed to update correlation configs");
            throw new APIManagementException("Failed to update correlation configs", e);

        }
    }

    public List<CorrelationConfigDTO> getCorrelationConfigsList() throws APIManagementException {
        List<CorrelationConfigDTO> correlationConfigDTOList = new ArrayList<>();
        String queryConfigs = SQLConstants.RETRIEVE_CORRELATION_CONFIGS;
        String queryProps = SQLConstants.RETRIEVE_CORRELATION_CONFIG_PROPERTIES;

        try (Connection connection = APIMgtDBUtil.getConnection();
                PreparedStatement preparedStatementConfigs = connection.prepareStatement(queryConfigs);
                PreparedStatement preparedStatementProps = connection.prepareStatement(queryProps)) {
            try (ResultSet resultSetConfigs = preparedStatementConfigs.executeQuery()) {
                while (resultSetConfigs.next()) {
                    String componentName = resultSetConfigs.getString(COMPONENT_NAME);
                    String enabled = resultSetConfigs.getString(ENABLED);


                    preparedStatementProps.setString(1, componentName);
                    try (ResultSet resultSetProps = preparedStatementProps.executeQuery()) {

                        List<CorrelationConfigPropertyDTO> correlationConfigPropertyDTOList = new ArrayList<>();
                        while (resultSetProps.next()) {
                            String propertyName = resultSetProps.getString(PROPERTY_NAME);
                            String propertyValue = resultSetProps.getString(PROPERTY_VALUE);
                            CorrelationConfigPropertyDTO correlationConfigPropertyDTO =
                                    new CorrelationConfigPropertyDTO(propertyName, propertyValue.split(","));
                            correlationConfigPropertyDTOList.add(correlationConfigPropertyDTO);
                        }

                        CorrelationConfigDTO correlationConfigDTO = new CorrelationConfigDTO(componentName, enabled,
                                correlationConfigPropertyDTOList);
                        correlationConfigDTOList.add(correlationConfigDTO);
                    }
                }
            }
        } catch (SQLException e) {
            throw new APIManagementException("Failed to retrieve correlation component configs", e);
        }
        return correlationConfigDTOList;
    }

    public boolean isConfigExist() throws APIManagementException {
        String queryConfigs = SQLConstants.RETRIEVE_CORRELATION_CONFIGS;
        try (Connection connection = APIMgtDBUtil.getConnection();
                PreparedStatement preparedStatementConfigs = connection.prepareStatement(queryConfigs);
                ResultSet resultSetConfigs = preparedStatementConfigs.executeQuery()) {

            if (resultSetConfigs != null && resultSetConfigs.next()) {
                return true;
            }
            return false;

        } catch (SQLException e) {
            throw new APIManagementException("Error while retrieving correlation configs" , e);
        }
    }

    public void addDefaultCorrelationConfigs() throws APIManagementException {
        String queryConfigs = SQLConstants.INSERT_CORRELATION_CONFIGS;
        String queryProps = SQLConstants.INSERT_CORRELATION_CONFIG_PROPERTIES;
        try (Connection connection = APIMgtDBUtil.getConnection()) {
            try (PreparedStatement preparedStatementConfigs = connection.prepareStatement(queryConfigs);
                    PreparedStatement preparedStatementProps = connection.prepareStatement(queryProps)) {

                connection.setAutoCommit(false);
                log.debug("Inserting into Correlation Configs");
                for (String componentName : DEFAULT_CORRELATION_COMPONENTS) {
                    String enabled = "false";

                    preparedStatementConfigs.setString(1, componentName);
                    preparedStatementConfigs.setString(2, enabled);
                    preparedStatementConfigs.addBatch();
                }
                preparedStatementConfigs.executeBatch();

                preparedStatementProps.setString(1, DENIED_THREADS);
                preparedStatementProps.setString(2, "jdbc");
                preparedStatementProps.setString(3, DEFAULT_DENIED_THREADS);
                preparedStatementProps.executeUpdate();
                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                throw new APIManagementException("Error while updating the correlation configs", e);
            }
        } catch (SQLException e) {
            throw new APIManagementException("Error while updating the correlation configs", e);
        }

    }
}
