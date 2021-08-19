package org.eclipse.tesla.aether.localrepo.split;

/*******************************************************************************
 * Copyright (c) 2011, 2021 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalMetadataResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
class SplitLocalRepositoryManager
    implements LocalRepositoryManager
{

    static final String LOCAL_SNAPSHOTS = "ls/";

    static final String LOCAL_RELEASES = "lr/";

    static final String REMOTE_SNAPSHOTS = "rs/";

    static final String REMOTE_RELEASES = "rr/";

    private static final Logger logger = LoggerFactory.getLogger(SplitLocalRepositoryManager.class);

    private final LocalRepository repository;

    private TrackingFileManager trackingFileManager;


    public SplitLocalRepositoryManager( File basedir )
    {
        if ( basedir == null )
        {
            throw new IllegalArgumentException( "base directory has not been specified" );
        }
        repository = new LocalRepository( basedir.getAbsoluteFile(), "splitted" );
        trackingFileManager = new TrackingFileManager();
    }

    public LocalRepository getRepository()
    {
        return repository;
    }

    private String getSubPath( Artifact artifact, boolean local )
    {
        StringBuilder path = new StringBuilder( 128 );

        path.append( artifact.getGroupId().replace( '.', '/' ) ).append( '/' );

        path.append( artifact.getArtifactId() ).append( '/' );

        path.append( artifact.getBaseVersion() ).append( '/' );

        path.append( artifact.getArtifactId() ).append( '-' );
        if ( local )
        {
            path.append( artifact.getBaseVersion() );
        }
        else
        {
            path.append( artifact.getVersion() );
        }

        if ( artifact.getClassifier().length() > 0 )
        {
            path.append( '-' ).append( artifact.getClassifier() );
        }

        if ( artifact.getExtension().length() > 0 )
        {
            path.append( '.' ).append( artifact.getExtension() );
        }

        return path.toString();
    }

    public String getPathForLocalArtifact( Artifact artifact )
    {
        String path = artifact.isSnapshot() ? LOCAL_SNAPSHOTS : LOCAL_RELEASES;
        path += getSubPath( artifact, true );
        return path;
    }

    public String getPathForRemoteArtifact( Artifact artifact, RemoteRepository repository, String context )
    {
        return getPathForRemoteArtifact( artifact );
    }

    private String getPathForRemoteArtifact( Artifact artifact )
    {
        String path = artifact.isSnapshot() ? REMOTE_SNAPSHOTS : REMOTE_RELEASES;
        path += getSubPath( artifact, false );
        return path;
    }

    private String getSubPath( Metadata metadata, String repositoryKey )
    {
        StringBuilder path = new StringBuilder( 128 );

        if ( metadata.getGroupId().length() > 0 )
        {
            path.append( metadata.getGroupId().replace( '.', '/' ) ).append( '/' );

            if ( metadata.getArtifactId().length() > 0 )
            {
                path.append( metadata.getArtifactId() ).append( '/' );

                if ( metadata.getVersion().length() > 0 )
                {
                    path.append( metadata.getVersion() ).append( '/' );
                }
            }
        }

        path.append( insertRepositoryKey( metadata.getType(), repositoryKey ) );

        return path.toString();
    }

    private String insertRepositoryKey( String filename, String repositoryKey )
    {
        String result;
        int idx = filename.indexOf( '.' );
        if ( repositoryKey == null || repositoryKey.length() <= 0 )
        {
            result = filename;
        }
        else if ( idx < 0 )
        {
            result = filename + '-' + repositoryKey;
        }
        else
        {
            result = filename.substring( 0, idx ) + '-' + repositoryKey + filename.substring( idx );
        }
        return result;
    }

    private boolean isSnapshot( Metadata metadata )
    {
        return metadata.getVersion().endsWith( "SNAPSHOT" );
    }

    private String[] getPaths( Metadata metadata, String repositoryKey, String releases, String snapshots )
    {
        if ( repositoryKey == null )
        {
            repositoryKey = getRepository().getId();
        }

        String path = getSubPath( metadata, repositoryKey );
        String[] paths;
        if ( metadata.getVersion().length() > 0 )
        {
            paths = new String[] { ( isSnapshot( metadata ) ? snapshots : releases ) + path };
        }
        else
        {
            // 1st path = master path, 2nd path = backup path
            paths = new String[] { releases + path, snapshots + path };
        }
        return paths;
    }

    public String getPathForLocalMetadata( Metadata metadata )
    {
        return getPaths( metadata, null, LOCAL_RELEASES, LOCAL_SNAPSHOTS )[0];
    }

    public String getPathForRemoteMetadata( Metadata metadata, RemoteRepository repository, String context )
    {
        return getPaths( metadata, getRepositoryKey( repository, context ), REMOTE_RELEASES, REMOTE_SNAPSHOTS )[0];
    }

    private String getRepositoryKey( RemoteRepository repository, String context )
    {
        String key;

        if ( repository.isRepositoryManager() )
        {
            // repository serves dynamic contents, take request parameters into account for key

            StringBuilder buffer = new StringBuilder( 128 );

            buffer.append( repository.getId() );

            buffer.append( '-' );

            SortedSet<String> subKeys = new TreeSet<String>();
            for ( RemoteRepository mirroredRepo : repository.getMirroredRepositories() )
            {
                subKeys.add( mirroredRepo.getId() );
            }

            SimpleDigest digest = new SimpleDigest();
            digest.update( context );
            for ( String subKey : subKeys )
            {
                digest.update( subKey );
            }
            buffer.append( digest.digest() );

            key = buffer.toString();
        }
        else
        {
            // repository serves static contents, its id is sufficient as key

            key = repository.getId();
        }

        return key;
    }

    public LocalArtifactResult find( RepositorySystemSession session, LocalArtifactRequest request )
    {
        LocalArtifactResult result = new LocalArtifactResult( request );

        String subPath = getSubPath( request.getArtifact(), false );

        String path = request.getArtifact().isSnapshot() ? LOCAL_SNAPSHOTS : LOCAL_RELEASES;
        path += subPath;
        File file = new File( getRepository().getBasedir(), path );

        if ( file.isFile() )
        {
            result.setFile( file );
            result.setAvailable( true );
        }
        else
        {
            path = request.getArtifact().isSnapshot() ? REMOTE_SNAPSHOTS : REMOTE_RELEASES;
            path += subPath;
            file = new File( getRepository().getBasedir(), path );

            if ( file.isFile() )
            {
                result.setFile( file );

                Properties props = readRepos( file );

                String context = request.getContext();
                for ( RemoteRepository repository : request.getRepositories() )
                {
                    if ( props.get( getKey( file, getRepositoryKey( repository, context ) ) ) != null )
                    {
                        result.setAvailable( true );
                        result.setRepository( repository );
                        break;
                    }
                }
            }
        }

        return result;
    }

    public void add( RepositorySystemSession session, LocalArtifactRegistration request )
    {
        if ( request.getRepository() != null )
        {
            addArtifact( request.getArtifact(), getRepositoryKeys( request.getRepository(), request.getContexts() ) );
        }
    }

    private Collection<String> getRepositoryKeys( RemoteRepository repository, Collection<String> contexts )
    {
        Collection<String> keys = new HashSet<String>();

        if ( contexts != null )
        {
            for ( String context : contexts )
            {
                keys.add( getRepositoryKey( repository, context ) );
            }
        }

        return keys;
    }

    private void addArtifact( Artifact artifact, Collection<String> repositories )
    {
        if ( artifact == null )
        {
            throw new IllegalArgumentException( "artifact to register not specified" );
        }
        String path = getPathForRemoteArtifact( artifact );
        File file = new File( getRepository().getBasedir(), path );
        addRepo( file, repositories );
    }

    private Properties readRepos( File artifactFile )
    {
        File trackingFile = getTrackingFile( artifactFile );

        Properties props = trackingFileManager.read( trackingFile );
        return ( props != null ) ? props : new Properties();
    }

    private void addRepo( File artifactFile, Collection<String> repositories )
    {
        Map<String, String> updates = new HashMap<String, String>();
        for ( String repository : repositories )
        {
            updates.put( getKey( artifactFile, repository ), "" );
        }

        File trackingFile = getTrackingFile( artifactFile );

        trackingFileManager.update( trackingFile, updates );
    }

    private File getTrackingFile( File artifactFile )
    {
        return new File( artifactFile.getParentFile(), "_maven.repositories" );
    }

    private String getKey( File file, String repository )
    {
        return file.getName() + '>' + repository;
    }

    public LocalMetadataResult find( RepositorySystemSession session, LocalMetadataRequest request )
    {
        LocalMetadataResult result = new LocalMetadataResult( request );

        String[] paths;

        Metadata metadata = request.getMetadata();
        String context = request.getContext();
        RemoteRepository remote = request.getRepository();

        if ( remote != null )
        {
            paths = getPaths( metadata, getRepositoryKey( remote, context ), REMOTE_RELEASES, REMOTE_SNAPSHOTS );
        }
        else
        {
            paths = getPaths( metadata, null, LOCAL_RELEASES, LOCAL_SNAPSHOTS );
        }

        if ( paths.length < 2 )
        {
            File file = new File( getRepository().getBasedir(), paths[0] );
            if ( file.isFile() )
            {
                result.setFile( file );
            }
        }
        else
        {
            File masterFile = new File( getRepository().getBasedir(), paths[0] );
            File backupFile = new File( getRepository().getBasedir(), paths[1] );
            boolean masterExists = masterFile.isFile();
            boolean backupExists = backupFile.isFile();
            if ( masterExists || backupExists )
            {
                try
                {
                    if ( !backupExists )
                    {
                        logger.debug( "Repairing metadata file " + masterFile + " after deletion of snapshots" );
                        MetadataUtils.repair( metadata, masterFile, masterFile, true, false );
                        backupMetadata( paths );
                    }
                    else if ( !masterExists )
                    {
                        logger.debug( "Repairing metadata file " + masterFile + " after deletion of releases" );
                        MetadataUtils.repair( metadata, backupFile, masterFile, false, true );
                    }
                }
                catch ( IOException e )
                {
                    logger.warn( "Could not repair metadata file " + masterFile + ": " + e );
                }

                if ( masterExists || masterFile.isFile() )
                {
                    result.setFile( masterFile );
                    result.setStale( !masterExists || !backupExists );
                }
            }
        }

        return result;
    }

    public void add( RepositorySystemSession session, LocalMetadataRegistration request )
    {
        Metadata metadata = request.getMetadata();

        if ( metadata.getVersion().length() <= 0 )
        {
            if ( request.getRepository() == null )
            {
                backupMetadata( getPaths( metadata, null, LOCAL_RELEASES, LOCAL_SNAPSHOTS ) );
            }
            else
            {
                for ( String repositoryKey : getRepositoryKeys( request.getRepository(), request.getContexts() ) )
                {
                    backupMetadata( getPaths( metadata, repositoryKey, REMOTE_RELEASES, REMOTE_SNAPSHOTS ) );
                }
            }
        }
    }

    private void backupMetadata( String[] paths )
    {
        File basedir = getRepository().getBasedir();
        File masterFile = new File( basedir, paths[0] );
        File backupFile = new File( basedir, paths[1] );

        try
        {
            FileUtils.copyFile( masterFile, backupFile );
        }
        catch ( IOException e )
        {
            logger.warn( "Could not create metadata backup file " + backupFile + ": " + e );
        }
    }

}
