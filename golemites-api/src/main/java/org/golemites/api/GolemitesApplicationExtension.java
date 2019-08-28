package org.golemites.api;

import lombok.Data;

@Data
public class GolemitesApplicationExtension {
    private String namespace = "default";
    private String name; // TODO: Default to project.name?
    private PushTarget pushTo = PushTarget.NONE;
    private String repository;
}
