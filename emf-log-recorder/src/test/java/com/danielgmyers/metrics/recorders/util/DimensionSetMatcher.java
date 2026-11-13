/*
 *   Copyright Metric Recorder Contributors
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.danielgmyers.metrics.recorders.util;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.jupiter.api.Assertions;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;

import java.util.HashMap;
import java.util.Map;

public class DimensionSetMatcher implements IArgumentMatcher {

    private final Map<String, String> targetDimensions;

    DimensionSetMatcher(DimensionSet targetDimensionSet) {
        this.targetDimensions = new HashMap<>();
            targetDimensionSet.getDimensionKeys().forEach((dimName) -> {
            String value = targetDimensionSet.getDimensionValue(dimName);
            targetDimensions.put(dimName, value);
        });
    }

    @Override
    public boolean matches(Object o) {
        if (!(o instanceof DimensionSet)) {
            return false;
        }
        DimensionSet actual = (DimensionSet)o;
        Map<String, String> expectedDimensions = new HashMap<>(targetDimensions);

        actual.getDimensionKeys().forEach((dimName) -> {
            Assertions.assertTrue(expectedDimensions.containsKey(dimName));
            String actualDim = actual.getDimensionValue(dimName);
            Assertions.assertEquals(actualDim, expectedDimensions.get(dimName));
            expectedDimensions.remove(dimName);
        });
        Assertions.assertTrue(expectedDimensions.isEmpty());
        return true;
    }

    @Override
    public void appendTo(StringBuffer stringBuffer) {
        stringBuffer.append("== [");
        stringBuffer.append(targetDimensions.toString());
        stringBuffer.append("]");
    }

    public static DimensionSet eqDimensionSet(Map<String, ?> dimensions) {
        DimensionSet dimset = new DimensionSet();
        dimensions.forEach((key, value) -> dimset.addDimension(key, value.toString()));
        EasyMock.reportMatcher(new DimensionSetMatcher(dimset));
        return null;
    }
}
