package net.bteuk.network;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.List;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class PluginLibrariesLoader implements PluginLoader {
    @Override
    public void classloader(@NonNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        // 1. Load terraminusminus while excluding netty-common to prevent duplicate ConstantPool initialization
        DefaultArtifact terraArtifact = new DefaultArtifact("net.buildtheearth.terraminusminus:terraminusminus-bukkit:2.2.1-1.21.8");
        Exclusion nettyCommonExclusion = new Exclusion("io.netty", "netty-common", "*", "*");

        resolver.addDependency(new Dependency(terraArtifact, null, false, List.of(nettyCommonExclusion)));

        // 2. Explicitly add netty-resolver-dns (with netty-common excluded) to avoid NoClassDefFoundError
        DefaultArtifact dnsArtifact = new DefaultArtifact("io.netty:netty-resolver-dns:4.1.100.Final");
        resolver.addDependency(new Dependency(dnsArtifact, null, false, List.of(nettyCommonExclusion)));

        // Repositories
        resolver.addRepository(new RemoteRepository.Builder("reposilite-repository-releases", "default", "https://maven.buildtheearth.net/releases").build());
        resolver.addRepository(new RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());

        classpathBuilder.addLibrary(resolver);
    }
}