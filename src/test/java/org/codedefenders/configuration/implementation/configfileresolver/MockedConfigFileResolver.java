/*
 * Copyright (C) 2020 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */

package org.codedefenders.configuration.implementation.configfileresolver;

import org.codedefenders.configuration.implementation.ConfigFileResolver;

import java.io.Reader;
import java.io.StringReader;

public class MockedConfigFileResolver extends ConfigFileResolver {
    @Override
    public Reader getConfigFile(String filename) {
        String config;
        if (filename.endsWith(".properties")) {
            config = "db.name = codedefenders\n";
        } else if (filename.endsWith(".properties.yml")){
            config =  "db:\n"
                    + "  name: codedefenders";
        } else {
            return null;
        }
        return new StringReader(config);
    }
}
