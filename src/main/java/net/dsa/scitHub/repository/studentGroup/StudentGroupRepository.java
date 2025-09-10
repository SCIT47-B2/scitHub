package net.dsa.scitHub.repository.studentGroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.dsa.scitHub.entity.studentGroup.StudentGroup;
import net.dsa.scitHub.enums.ClassSection;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Integer> {
    
    /** 기수별 그룹 조회 */
    List<StudentGroup> findByCohortNo(Integer cohortNo);
    
    /** 기수와 반으로 조회 */
    List<StudentGroup> findByCohortNoAndClassSection(Integer cohortNo, ClassSection classSection);
    
    /** 기수별 그룹을 순서대로 조회 */
    List<StudentGroup> findByCohortNoOrderByOrderIndex(Integer cohortNo);
    
    /** 그룹명으로 검색 */
    List<StudentGroup> findByNameContaining(String name);
    
    /** 정확한 그룹명으로 조회 */
    Optional<StudentGroup> findByName(String name);
    
    /** 반별 그룹 조회 */
    List<StudentGroup> findByClassSection(ClassSection classSection);
    
    /** 기수별 반별 그룹 수 조회 */
    @Query("SELECT sg.cohortNo, sg.classSection, COUNT(sg) FROM StudentGroup sg " +
           "GROUP BY sg.cohortNo, sg.classSection ORDER BY sg.cohortNo DESC, sg.classSection")
    List<Object[]> countGroupsByCohortAndSection();
    
    /** 멤버 수와 함께 그룹 조회 */
    @Query("SELECT sg, COUNT(u) FROM StudentGroup sg LEFT JOIN sg.users u GROUP BY sg ORDER BY COUNT(u) DESC")
    List<Object[]> findGroupsWithMemberCount();
    
    /** 특정 기수의 최대 순서 인덱스 조회 */
    @Query("SELECT MAX(sg.orderIndex) FROM StudentGroup sg WHERE sg.cohortNo = :cohortNo")
    Integer findMaxOrderIndexByCohortNo(@Param("cohortNo") Integer cohortNo);
    
    /** 멤버가 있는 그룹만 조회 */
    @Query("SELECT DISTINCT sg FROM StudentGroup sg JOIN sg.users u")
    List<StudentGroup> findGroupsWithMembers();
}
