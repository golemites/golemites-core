package org.tablerocket.febo.api;

import lombok.Data;

@Data
public class FeboApplicationExtension {
    private PushTarget pushTo = PushTarget.DOCKER_DAEMON;
    private String repository;
    private boolean deployImage = false;
}
