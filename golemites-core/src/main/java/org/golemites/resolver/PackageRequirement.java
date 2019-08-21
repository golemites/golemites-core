package org.golemites.resolver;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class PackageRequirement implements Requirement
{
    private final Resource m_resource;
    private final Map<String, String> m_dirs;
    private final Map<String, Object> m_attrs;

    public PackageRequirement(Resource resource, String name)
    {
        m_resource = resource;
        m_dirs = new HashMap<String, String>();
        m_dirs.put(
                PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
                "(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + name + ")");
        m_attrs = new HashMap<String, Object>();
    }

    public String getNamespace()
    {
        return PackageNamespace.PACKAGE_NAMESPACE;
    }

    public Map<String, String> getDirectives()
    {
        return m_dirs;
    }

    public Map<String, Object> getAttributes()
    {
        return m_attrs;
    }

    public Resource getResource()
    {
        return m_resource;
    }

    @Override
    public String toString()
    {
        return getNamespace() + "; "
                + getDirectives().get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE).toString();
    }
}
