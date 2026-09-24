package com.quickflow.domain.task;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the task list query from a {@link TaskQuery}. The order is set here (not through a {@code Sort}) so
 * null due dates are last in both directions without relying on the provider's null precedence (FR-02.3).
 */
public final class TaskSpecifications {

	private static final char ESCAPE = '\\';

	private TaskSpecifications() {
	}

	public static Specification<Task> matching(TaskQuery query) {
		return (root, cq, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (query.q() != null && !query.q().isBlank()) {
				String pattern = "%" + escape(query.q().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(cb.like(cb.lower(root.get("title")), pattern, ESCAPE));
			}
			if (query.status() != null) {
				predicates.add(cb.equal(root.get("status"), query.status()));
			}
			if (query.priority() != null) {
				predicates.add(cb.equal(root.get("priority"), query.priority()));
			}
			Path<LocalDate> dueDate = root.get("dueDate");
			if (query.dueFrom() != null) {
				predicates.add(cb.greaterThanOrEqualTo(dueDate, query.dueFrom()));
			}
			if (query.dueTo() != null) {
				predicates.add(cb.lessThanOrEqualTo(dueDate, query.dueTo()));
			}
			predicates.add(cb.equal(root.get("archived"), query.archived()));
			if (cq != null) {
				cq.orderBy(orders(query, root, cb));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static List<Order> orders(TaskQuery query, Root<Task> root, CriteriaBuilder cb) {
		boolean asc = query.direction() != Sort.Direction.DESC;
		List<Order> orders = new ArrayList<>();
		if (query.sort() == TaskSort.DUE_DATE) {
			Expression<Integer> nullsLast = cb.<Integer>selectCase()
					.when(cb.isNull(root.get("dueDate")), 1)
					.otherwise(0);
			orders.add(cb.asc(nullsLast));
			orders.add(order(cb, root.get("dueDate"), asc));
		}
		else {
			orders.add(order(cb, root.get("createdAt"), asc));
		}
		orders.add(order(cb, root.get("id"), asc));
		return orders;
	}

	private static Order order(CriteriaBuilder cb, Expression<?> expression, boolean asc) {
		return asc ? cb.asc(expression) : cb.desc(expression);
	}

	private static String escape(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
