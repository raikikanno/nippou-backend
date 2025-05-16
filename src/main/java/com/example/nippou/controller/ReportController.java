package com.example.nippou.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.nippou.model.Report;
import com.example.nippou.repository.ReportRepository;

import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:3000")

public class ReportController {

    private final ReportRepository reportRepository;

    public ReportController(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @GetMapping
    public List<Report> getReports() {
        return reportRepository.findAll();
    }

    @PostMapping
    public Report createReport(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "新しい日報",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @Schema(implementation = Report.class)
                    )
            ) Report report
    ) {
        return reportRepository.save(report);
    }

    @PutMapping("/{id}")
    public Report updateReport(@PathVariable String id, @RequestBody Report updatedReport) {
        return reportRepository.save(updatedReport);
    }

    @DeleteMapping("/{id}")
    public void deleteReport(@PathVariable String id) {
        reportRepository.deleteById(id);
    }
}
