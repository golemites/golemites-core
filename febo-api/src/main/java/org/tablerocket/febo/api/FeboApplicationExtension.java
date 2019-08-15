package org.tablerocket.febo.api;

import lombok.Data;

@Data
public class FeboApplicationExtension {
    private String repository;
    private boolean deployImage = false;
}
