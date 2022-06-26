package com.mars.ecsheet.repository;

import com.mars.ecsheet.entity.WorkSheetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Mars
 * @date 2020/10/29
 * @description
 */
@Repository
public interface WorkSheetRepository extends JpaRepository<WorkSheetEntity, String> {


    @Query(value = "SELECT id, workbook_id, `data`, delete_status " +
            "FROM worksheet where workbook_id=:wbId and delete_status=0",nativeQuery = true)
    List<WorkSheetEntity> findAllBywbId(@Param("wbId") String wbId);

    @Query(value = "SELECT id, workbook_id, `data`, delete_status  " +
            "FROM worksheet  where workbook_id=:wbId and data ->>'$.index' = :index",nativeQuery = true)
    WorkSheetEntity findByindexAndwbId(@Param("index")String index, @Param("wbId")String wbId);


    @Query(value = "SELECT id, workbook_id, `data`, delete_status " +
            "FROM worksheet  where workbook_id=:wbId and data ->>'$.status' = :status",nativeQuery = true)
    List<WorkSheetEntity> findBystatusAndwbId(@Param("status")int status, @Param("wbId")String wbId);


}
