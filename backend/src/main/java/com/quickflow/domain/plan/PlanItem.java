package com.quickflow.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One source of a plan. No foreign key to the source tables: a source can be deleted while the item stays, showing
 * its last title (FR-07.6).
 */
@Entity
@Table(name = "plan_item",
		uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "source_type", "source_id"}),
		indexes = @Index(name = "idx_plan_item_plan_id", columnList = "plan_id"))
public class PlanItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id", nullable = false)
	private Plan plan;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false)
	private PlanSourceType sourceType;

	@Column(name = "source_id", nullable = false)
	private Long sourceId;

	@Column(name = "source_title", nullable = false, length = Plan.TITLE_MAX)
	private String sourceTitle;

	/** Independent of the plan status (data model). */
	@Column(nullable = false)
	private boolean done;

	protected PlanItem() {
	}

	/** A new item, not done (FR-07.2); the service stores the source title before saving. */
	PlanItem(Plan plan, PlanSourceType sourceType, Long sourceId) {
		this.plan = plan;
		this.sourceType = sourceType;
		this.sourceId = sourceId;
		this.done = false;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	/** Stores the source's current title as the snapshot shown once the source is deleted (A-4). */
	public void refreshTitle(String title) {
		this.sourceTitle = title;
	}

	public Long getId() {
		return id;
	}

	public Plan getPlan() {
		return plan;
	}

	public Long getPlanId() {
		return plan.getId();
	}

	public PlanSourceType getSourceType() {
		return sourceType;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public String getSourceTitle() {
		return sourceTitle;
	}

	public boolean isDone() {
		return done;
	}

}
