/*
 *  Copyright 2020 Johnny850807 (Waterball) 潘冠辰
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tw.waterball.judgegirl.springboot.problem.repositories;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;
import tw.waterball.judgegirl.commons.models.files.FileResource;
import tw.waterball.judgegirl.entities.problem.Problem;
import tw.waterball.judgegirl.problemservice.domain.repositories.ProblemQueryParams;
import tw.waterball.judgegirl.problemservice.domain.repositories.ProblemRepository;
import tw.waterball.judgegirl.springboot.profiles.productions.Mongo;
import tw.waterball.judgegirl.springboot.utils.MongoUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static tw.waterball.judgegirl.springboot.utils.MongoUtils.downloadFileResourceByFileId;

/**
 * @author - johnny850807@gmail.com (Waterball)
 */
@Mongo
@Component
public class MongoProblemRepository implements ProblemRepository {
    private final static int PAGE_SIZE = 50;
    private MongoTemplate mongoTemplate;
    private GridFsTemplate gridFsTemplate;

    public MongoProblemRepository(MongoTemplate mongoTemplate, GridFsTemplate gridFsTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.gridFsTemplate = gridFsTemplate;
    }

    @Override
    public Optional<Problem> findProblemById(int problemId) {
        return Optional.ofNullable(mongoTemplate.findById(problemId, Problem.class));
    }

    @Override
    public Optional<FileResource> downloadZippedProvidedCodes(int problemId) {
        return MongoUtils.query(mongoTemplate)
                .fromDocument(Problem.class)
                .selectOneField("providedCodesFileId")
                .byId(problemId)
                .execute()
                .getField(Problem::getProvidedCodesFileId)
                .map((fileId) -> downloadFileResourceByFileId(gridFsTemplate, fileId));
    }

    @Override
    public List<Problem> find(ProblemQueryParams params) {
        Query query = new Query();

        if (params.getTags().length > 0) {
            query.addCriteria(new Criteria("tags")
                    .all((Object[]) params.getTags()));
        }
        params.getPage().ifPresent(page -> query.skip(page * PAGE_SIZE).limit(PAGE_SIZE));

        return mongoTemplate.find(query, Problem.class);
    }

    @Override
    public int getPageSize() {
        return PAGE_SIZE;
    }

    @Override
    public List<Problem> findAll() {
        return mongoTemplate.findAll(Problem.class);
    }

    @Override
    public List<String> getTags() {
        AllTags tags = mongoTemplate.findOne(
                new Query(new Criteria()), AllTags.class);
        return tags == null ? Collections.emptyList() : tags.all;
    }

    @Document("tag")
    public static class AllTags {
        public List<String> all;

        public AllTags() {
        }

        public AllTags(List<String> all) {
            this.all = all;
        }
    }

}