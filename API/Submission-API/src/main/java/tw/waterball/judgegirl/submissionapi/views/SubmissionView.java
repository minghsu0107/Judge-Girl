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

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import tw.waterball.judgegirl.entities.submission.Submission;
import tw.waterball.judgegirl.entities.submission.Verdict;

import java.util.Date;

/**
 * @author - johnny850807@gmail.com (Waterball)
 */
@Data
@NoArgsConstructor
public class SubmissionView {
    public String id;
    public int studentId;
    public int problemId;
    public VerdictView verdict;
    public boolean isJudged;
    public String submittedCodesFileId;
    public Date submissionTime;

    public SubmissionView(String id, int studentId, int problemId, VerdictView verdict,
                          String submittedCodesFileId, Date submissionTime) {
        this.id = id;
        this.studentId = studentId;
        this.problemId = problemId;
        this.verdict = verdict;
        this.isJudged = verdict != null;
        this.submittedCodesFileId = submittedCodesFileId;
        this.submissionTime = submissionTime;
    }

    public static SubmissionView fromEntity(@Nullable Submission submission) {
        if (submission == null) {
            return null;
        }
        Verdict verdict = submission.getVerdict().orElse(null);
        return new SubmissionView(submission.getId(),
                submission.getStudentId(),
                submission.getProblemId(),
                VerdictView.fromEntity(verdict),
                submission.getSubmittedCodesFileId(),
                submission.getSubmissionTime());
    }

    public static Submission toEntity(@Nullable SubmissionView submissionView) {
        if (submissionView == null) {
            return null;
        }

        VerdictView verdictView = submissionView.getVerdict();
        Submission submission = new Submission(submissionView.getId(),
                submissionView.getStudentId(),
                submissionView.getProblemId(),
                submissionView.getSubmittedCodesFileId());

        Verdict verdict = VerdictView.toEntity(verdictView);
        submission.setVerdict(verdict);
        return submission;
    }
}
