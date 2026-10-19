package com.hims.info.mapper;

import org.apache.ibatis.annotations.Param;

public interface UserPatientRelationMapper {
    int insertRelation(@Param("userId") Long userId, @Param("patientId") Long patientId);

    boolean existsRelation(@Param("userId") Long userId, @Param("patientId") Long patientId);
}
