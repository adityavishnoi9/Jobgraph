package com.JobGraph.jobgraph_backend.service;

import com.JobGraph.jobgraph_backend.DTO.*;
import com.JobGraph.jobgraph_backend.model.*;
import com.JobGraph.jobgraph_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final ExperienceRepository experienceRepository;
    private final ProjectRepository projectRepository;
    private final BulletRepository bulletRepository;

    public ProfileService(UserRepository userRepository,
                          ExperienceRepository experienceRepository,
                          ProjectRepository projectRepository,
                          BulletRepository bulletRepository) {
        this.userRepository = userRepository;
        this.experienceRepository = experienceRepository;
        this.projectRepository = projectRepository;
        this.bulletRepository = bulletRepository;
    }

    /**
     * Saves the confirmed/edited profile (from the Gemini-parsed JSON, after
     * the user has reviewed it on the frontend) into all 4 tables.
     *
     * Order matters here:
     *  1. Save the user first (need its generated id for foreign keys).
     *  2. For each experience/project, save it (need ITS generated id too).
     *  3. Only then save each bullet, since a bullet's source_id points to
     *     the experience/project row it belongs to - that id doesn't exist
     *     until step 2 is done.
     *
     * @Transactional ensures all of this succeeds or fails together - if
     * anything breaks halfway (e.g. a bad bullet), nothing partial gets
     * left in the database.
     */
    @Transactional
    public User saveProfile(ParsedProfileDto dto) {

        // Step 1: save the user
        User user = new User();
        user.setFname(dto.getFname());
        user.setLname(dto.getLname());
        user.setEmail(dto.getEmail());
        user.setLinkedinUrl(dto.getLinkedinUrl());
        user.setCodingHandles(dto.getCodingHandles());
        user.setSkills(dto.getSkills());
        user = userRepository.save(user);

        // Step 2 + 3: experiences and their bullets
        if (dto.getExperience() != null) {
            for (ExperienceDto expDto : dto.getExperience()) {
                Experience experience = new Experience();
                experience.setUser(user);
                experience.setCompanyName(expDto.getCompanyName());
                experience.setRole(expDto.getRole());
                experience.setStartDate(expDto.getStartDate());
                experience.setEndDate(expDto.getEndDate());
                experience = experienceRepository.save(experience);

                if (expDto.getBullets() != null) {
                    for (BulletDto bulletDto : expDto.getBullets()) {
                        Bullet bullet = new Bullet();
                        bullet.setUser(user);
                        bullet.setSourceType(Bullet.SourceType.experience);
                        bullet.setSourceId(experience.getId());
                        bullet.setText(bulletDto.getText());
                        bullet.setTags(bulletDto.getTags());
                        bulletRepository.save(bullet);
                    }
                }
            }
        }

        // Step 2 + 3: projects and their bullets
        if (dto.getProjects() != null) {
            for (ProjectDto projDto : dto.getProjects()) {
                Project project = new Project();
                project.setUser(user);
                project.setName(projDto.getName());
                project.setDescription(projDto.getDescription());
                project = projectRepository.save(project);

                if (projDto.getBullets() != null) {
                    for (BulletDto bulletDto : projDto.getBullets()) {
                        Bullet bullet = new Bullet();
                        bullet.setUser(user);
                        bullet.setSourceType(Bullet.SourceType.project);
                        bullet.setSourceId(project.getId());
                        bullet.setText(bulletDto.getText());
                        bullet.setTags(bulletDto.getTags());
                        bulletRepository.save(bullet);
                    }
                }
            }
        }

        return user;
    }
}