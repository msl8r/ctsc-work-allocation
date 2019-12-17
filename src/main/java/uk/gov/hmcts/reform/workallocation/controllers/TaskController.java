package uk.gov.hmcts.reform.workallocation.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.queue.QueueProducer;

import java.util.Collections;
import javax.validation.Valid;

@RestController
public class TaskController {

    private final QueueProducer<Task> queueProducer;

    public TaskController(QueueProducer<Task> queueProducer) {
        this.queueProducer = queueProducer;
    }

    @PostMapping("/task")
    public ResponseEntity<String> addTask(@Valid @RequestBody Task task) {

        queueProducer.placeItemsInQueue(Collections.singletonList(task), Task::getId);
        return ResponseEntity.ok("");
    }
}
