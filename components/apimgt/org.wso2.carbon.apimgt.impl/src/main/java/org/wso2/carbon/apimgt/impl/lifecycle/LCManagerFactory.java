/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.impl.lifecycle;

import org.json.simple.parser.ParseException;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.persistence.exceptions.PersistenceException;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.io.IOException;

/**
 * Factory class to get the LCManager
 */
public class LCManagerFactory {

    private static LCManagerFactory instance;

    private LCManagerFactory() {}

    /**
     * Returning the LCManager Factory instance
     * @return
     */
    public static LCManagerFactory getInstance() {
        if (instance == null) {
            instance = new LCManagerFactory();
        }
        return instance;
    }

    /**
     * Return new LCManager Instance
     * @return
     * @throws PersistenceException
     * @throws IOException
     * @throws ParseException
     */
    public LCManager getLCManager() throws PersistenceException, IOException, ParseException, APIManagementException {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        LCManager lcManager = new LCManager(tenantDomain);
        return lcManager;
    }
}