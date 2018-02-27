package org.tablerocket.febo.plugin.resolver;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;

@EqualsAndHashCode
@ToString
public class ArtifactDescriptor
{

    private final transient SingleArtifactResolver resolver;

    @Setter
    @Getter
    private String group;

    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private String version;

    @Setter
    @Getter
    private String classifier;

    @Getter
    @Setter
    private String extension;

    @Getter
    @Setter
    private String type;

    private transient File file;


    public ArtifactDescriptor(SingleArtifactResolver resolver) {
        this.resolver = resolver;
    }

    public ArtifactDescriptor(File file) {
        this.resolver = (desc) -> file;
    }

    public static ArtifactDescriptor parseGAV(String incoming, SingleArtifactResolver resolver) {
        System.out.println("Resolving " + incoming);
        int offset = incoming.indexOf(":mvn:");
        String repoString = incoming;
        // extracts deps like wrap:mvn:javax.portlet/portlet-api/2.0$Export-Package=javax.portlet.*;version=2.0
        // to: mvn:javax.portlet/portlet-api/2.0
        if (offset > 0) {
            repoString = repoString.substring(offset+1);
            // also cut off any modifiers:
            int modIdx = repoString.indexOf("$");
            if (modIdx > 0) {
                repoString = repoString.substring(0,modIdx);
            }

        }
        if (repoString.startsWith("mvn:")) {
            // resolve using gradle resolver!
            ArtifactDescriptor descriptor = new ArtifactDescriptor(resolver);

            String pureGAV = repoString.substring(4);
            String splitSeparator = splitSeparator(pureGAV);
            String[] parts = pureGAV.split(splitSeparator);
            if (parts.length > 2) {
                descriptor.setGroup(parts[0]);
                descriptor.setName(parts[1]);
                descriptor.setVersion(parts[2]);
            }
            if (parts.length > 3) {
                descriptor.setExtension(parts[3]);
            }
            if (parts.length > 4) {
                descriptor.setClassifier(parts[4]);
            }

            // seems like pax url has no type part? Default to extension == type.
            descriptor.setType(descriptor.getExtension());
            if (descriptor.getType() == null) {
                descriptor.setType("jar");
            }
            if (descriptor.getName() == null) {
                throw new IllegalArgumentException("name cannot be null: " + repoString + ". Original: " + incoming);
            }
            return descriptor;
        }
        throw new IllegalArgumentException("Only mvn urls supported: " + repoString + ". Original: " + incoming);

    }

    private static String splitSeparator(String pureGAV) {
        // workaround for buggy mvn urls found for example here: https://github.com/hibernate/hibernate-validator/pull/803
        if (pureGAV.contains(":")) {
            return ":";
        }else {
            return "/";
        }
    }

    public File resolve() {
        if (file == null) {
            file = resolver.resolve(this);
        }
        return file;
    }

}
