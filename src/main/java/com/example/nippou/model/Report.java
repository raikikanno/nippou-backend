package com.example.nippou.model;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Schema(description = "日報エンティティ")
@Entity
public class Report {

    @Id
    @Schema(description = "ID（UUIDや日時などでユニーク）", example = "1720000000000")
    private String id;

    @Schema(description = "投稿者のユーザーID", example = "user123")
    private String userId;

    @Schema(description = "投稿者の名前", example = "山田太郎")
    private String userName;

    @Schema(description = "所属チーム", example = "チームA")
    private String team;

    @Schema(description = "投稿日（yyyy-MM-dd）", example = "2025-04-25")
    private String date;

    @ElementCollection
    @Schema(description = "タグの一覧（複数可）", example = "[\"営業\", \"開発\"]")
    private List<String> tags;

    @Schema(description = "日報本文", example = "今日は顧客訪問と資料作成を行いました。", required = true)
    private String content;

    // デフォルトコンストラクタ
    public Report() {
    }

    // 全フィールドコンストラクタ
    public Report(String id, String userId, String userName, String team, String date, List<String> tags, String content) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.team = team;
        this.date = date;
        this.tags = tags;
        this.content = content;
    }

    // Getter・Setter（重要！）
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
