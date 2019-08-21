package org.golemites.api;

import lombok.Data;

@Data
public class GolemitesApplicationExtension {
    private PushTarget pushTo = PushTarget.DOCKER_DAEMON;
    private String repository;
    private boolean deployImage = false;
}
