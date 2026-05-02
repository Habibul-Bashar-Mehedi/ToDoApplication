package com.todoapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TaskTagId implements Serializable {

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "tag_id")
    private Long tagId;
}
