package org.golemites.api;

import lombok.Data;

@Data
public class Metadata
{
     private String groupId;

     private String artifactId;

     private String version;

     private String classifier;

     private String type;

     private String bundleSymbolicName;

     public static Metadata metadata(String groupId, String artifactId, String version, String classifier, String type, String bsn) {
          Metadata metadata = new Metadata();
          metadata.setGroupId(groupId);
          metadata.setArtifactId(artifactId);
          metadata.setVersion(version);
          metadata.setClassifier(classifier);
          metadata.setType(type);
          metadata.setBundleSymbolicName(bsn);
          return metadata;
     }
}
