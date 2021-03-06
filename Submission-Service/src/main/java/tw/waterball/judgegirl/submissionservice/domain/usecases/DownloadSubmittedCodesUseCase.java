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

package tw.waterball.judgegirl.submissionservice.domain.usecases;

import lombok.Value;
import tw.waterball.judgegirl.commons.exceptions.NotFoundException;
import tw.waterball.judgegirl.commons.models.files.FileResource;
import tw.waterball.judgegirl.entities.submission.Submission;
import tw.waterball.judgegirl.submissionservice.domain.repositories.SubmissionRepository;

import javax.inject.Named;

/**
 * @author - johnny850807@gmail.com (Waterball)
 */
@Named
public class DownloadSubmittedCodesUseCase extends BaseSubmissionUseCase {

    public DownloadSubmittedCodesUseCase(SubmissionRepository submissionRepository) {
        super(submissionRepository);
    }

    public FileResource execute(Request request) {
        Submission submission = doFindSubmission(request.studentId, request.submissionId);
        validateRequest(request, submission);
        return submissionRepository.downloadZippedSubmittedCodes(request.submissionId)
                .orElseThrow(() -> NotFoundException
                        .resource("submission").id(request.submissionId));
    }

    private void validateRequest(Request request, Submission submission) {
        if (!submission.getSubmittedCodesFileId()
                .equals(request.submittedCodesFileId)) {
            throw new IllegalArgumentException(
                    String.format("Invalid submitted codes' file id: %s.", request.submittedCodesFileId));
        }
    }

    @Value
    public static class Request {
        public int studentId;
        public String submissionId;
        public String submittedCodesFileId;
    }
}
