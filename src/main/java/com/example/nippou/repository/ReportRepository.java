package com.example.nippou.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.nippou.model.Report;

@Repository
public interface ReportRepository extends JpaRepository<Report, String>{

}