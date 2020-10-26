package com.tanaguru.repository;

import com.tanaguru.domain.entity.audit.Resource;
import com.tanaguru.domain.entity.audit.Scenario;
import com.tanaguru.domain.entity.membership.contract.Contract;
import com.tanaguru.domain.entity.membership.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author rcharre
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Collection<Project> findAllByContract(Contract contract);
    
    @Query("SELECT a FROM Resource a WHERE a.project = ?1")
    Collection<Resource> getResourcesFromProject(Project project);
    
    @Query("SELECT a FROM Scenario a WHERE a.project = ?1")
    Collection<Scenario> getScenarioFromProject(Project project);
    
}
