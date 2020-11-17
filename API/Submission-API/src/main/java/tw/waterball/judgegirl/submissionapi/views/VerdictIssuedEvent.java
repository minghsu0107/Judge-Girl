/*
 * Copyright 2020 Johnny850807 (Waterball) 潘冠辰
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package tw.waterball.judgegirl.submissionapi.views;

import lombok.*;
import org.jetbrains.annotations.Nullable;
import tw.waterball.judgegirl.entities.submission.CodeInspectionReport;
import tw.waterball.judgegirl.entities.submission.Judge;

import java.util.Date;
import java.util.List;

/**
 * @author - johnny850807@gmail.com (Waterball)
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerdictIssuedEvent {
    private int problemId;
    private String problemTitle;
    private String submissionId;

    @Nullable
    private String compileErrorMessage;
    private Date issueTime;

    @Nullable
    private CodeInspectionReport codeInspectionReport;

    @Singular
    private List<Judge> judges;
}