package com.JobGraph.jobgraph_backend.service;

import com.JobGraph.jobgraph_backend.DTO.*;
import com.JobGraph.jobgraph_backend.model.*;
import com.JobGraph.jobgraph_backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {
    private final UserRepository users;
    private final ExperienceRepository experiences;
    private final ProjectRepository projects;
    private final BulletRepository bullets;

    public ProfileService(UserRepository users, ExperienceRepository experiences,
                          ProjectRepository projects, BulletRepository bullets) {
        this.users = users;
        this.experiences = experiences;
        this.projects = projects;
        this.bullets = bullets;
    }

    public boolean exists(Integer accountId) { return users.findByAccount_Id(accountId).isPresent(); }

    @Transactional(readOnly = true)
    // Return the saved profile in the same shape used by the editor.
    public Optional<ParsedProfileDto> get(Integer accountId) {
        return users.findByAccount_Id(accountId).map(this::toDto);
    }

    @Transactional
    // Create the first profile or update the existing profile owned by this account.
    public ParsedProfileDto save(Account account, ParsedProfileDto dto) {
        User user = users.findByAccount_Id(account.getId()).orElseGet(() -> {
            User created = new User();
            created.setAccount(account);
            return created;
        });
        user.setFname(dto.getFname() == null ? "" : dto.getFname().trim());
        user.setLname(dto.getLname());
        // The resume's contact email does not change the login account's email.
        user.setEmail(dto.getEmail() == null || dto.getEmail().isBlank() ? account.getEmail() : dto.getEmail().trim());
        user.setLinkedinUrl(dto.getLinkedinUrl());
        user.setCodingHandles(dto.getCodingHandles());
        user.setSkills(dto.getSkills());
        user.setAdditionalSections(dto.getAdditionalSections());
        user = users.saveAndFlush(user);

        // Replace child rows so removed roles, projects, and bullets disappear on save.
        // The transaction keeps the old profile if any part of the new save fails.
        bullets.deleteAllForUser(user.getId());
        experiences.deleteAllForUser(user.getId());
        projects.deleteAllForUser(user.getId());

        if (dto.getExperience() != null) for (ExperienceDto item : dto.getExperience()) {
            Experience experience = new Experience();
            experience.setUser(user);
            experience.setCompanyName(item.getCompanyName() == null ? "" : item.getCompanyName());
            experience.setRole(item.getRole());
            experience.setStartDate(item.getStartDate());
            experience.setEndDate(item.getEndDate());
            experience = experiences.saveAndFlush(experience);
            saveBullets(user, Bullet.SourceType.experience, experience.getId(), item.getBullets());
        }
        if (dto.getProjects() != null) for (ProjectDto item : dto.getProjects()) {
            Project project = new Project();
            project.setUser(user);
            project.setName(item.getName() == null ? "" : item.getName());
            project.setDescription(item.getDescription());
            project = projects.saveAndFlush(project);
            saveBullets(user, Bullet.SourceType.project, project.getId(), item.getBullets());
        }
        return toDto(user);
    }

    private void saveBullets(User user, Bullet.SourceType type, Integer sourceId, List<BulletDto> items) {
        if (items == null) return;
        for (BulletDto item : items) {
            Bullet bullet = new Bullet();
            bullet.setUser(user);
            bullet.setSourceType(type);
            bullet.setSourceId(sourceId);
            bullet.setText(item.getText() == null ? "" : item.getText());
            bullet.setTags(item.getTags());
            bullets.save(bullet);
        }
    }

    private ParsedProfileDto toDto(User user) {
        ParsedProfileDto dto = new ParsedProfileDto();
        dto.setFname(user.getFname());
        dto.setLname(user.getLname());
        dto.setEmail(user.getEmail());
        dto.setLinkedinUrl(user.getLinkedinUrl());
        dto.setCodingHandles(user.getCodingHandles());
        dto.setSkills(user.getSkills());
        dto.setAdditionalSections(user.getAdditionalSections());
        // A bullet points to either a role or a project through type + source ID.
        Map<String, List<BulletDto>> bySource = bullets.findAllByUser_IdOrderByIdAsc(user.getId()).stream()
                .collect(Collectors.groupingBy(b -> b.getSourceType() + ":" + b.getSourceId(),
                        Collectors.mapping(b -> {
                            BulletDto item = new BulletDto();
                            item.setText(b.getText());
                            item.setTags(b.getTags());
                            return item;
                        }, Collectors.toList())));
        dto.setExperience(experiences.findAllByUser_IdOrderByIdAsc(user.getId()).stream().map(e -> {
            ExperienceDto item = new ExperienceDto();
            item.setCompanyName(e.getCompanyName());
            item.setRole(e.getRole());
            item.setStartDate(e.getStartDate());
            item.setEndDate(e.getEndDate());
            item.setBullets(bySource.getOrDefault("experience:" + e.getId(), List.of()));
            return item;
        }).toList());
        dto.setProjects(projects.findAllByUser_IdOrderByIdAsc(user.getId()).stream().map(p -> {
            ProjectDto item = new ProjectDto();
            item.setName(p.getName());
            item.setDescription(p.getDescription());
            item.setBullets(bySource.getOrDefault("project:" + p.getId(), List.of()));
            return item;
        }).toList());
        return dto;
    }
}
