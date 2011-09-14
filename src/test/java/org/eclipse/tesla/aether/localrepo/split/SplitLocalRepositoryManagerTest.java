package org.eclipse.tesla.aether.localrepo.split;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.LocalArtifactRegistration;
import org.sonatype.aether.repository.LocalArtifactRequest;
import org.sonatype.aether.repository.LocalArtifactResult;
import org.sonatype.aether.repository.LocalMetadataRegistration;
import org.sonatype.aether.repository.LocalMetadataRequest;
import org.sonatype.aether.repository.LocalMetadataResult;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.metadata.DefaultMetadata;

/**
 */
public class SplitLocalRepositoryManagerTest
{

    private static final String LR = SplitLocalRepositoryManager.LOCAL_RELEASES;

    private static final String LS = SplitLocalRepositoryManager.LOCAL_SNAPSHOTS;

    private static final String RR = SplitLocalRepositoryManager.REMOTE_RELEASES;

    private static final String RS = SplitLocalRepositoryManager.REMOTE_SNAPSHOTS;

    @Rule
    public TemporaryFolder localRepoDir = new TemporaryFolder();

    private SplitLocalRepositoryManager lrm;

    private DefaultRepositorySystemSession session;

    private RemoteRepository central;

    private Artifact newArtifact( String coords )
    {
        return new DefaultArtifact( coords );
    }

    private Metadata newMetadata( String coords )
    {
        Matcher m = Pattern.compile( "([^: ]+)(:([^: ]+)(:([^: ]+))?)?" ).matcher( coords );
        assertTrue( m.matches() );
        return new DefaultMetadata( m.group( 1 ), m.group( 3 ), m.group( 5 ), "maven-metadata.xml",
                                    Metadata.Nature.RELEASE_OR_SNAPSHOT );
    }

    private void create( File file )
        throws Exception
    {
        file.getAbsoluteFile().getParentFile().mkdirs();
        file.createNewFile();
    }

    private void create( File file, String resource )
        throws Exception
    {
        FileUtils.copyURLToFile( getClass().getResource( "/metadata/" + resource ), file );
    }

    private void assertXmlEqual( String expectedName, File actual )
        throws Exception
    {
        String expectedXml = IOUtil.toString( getClass().getResourceAsStream( "/metadata/" + expectedName ), "UTF-8" );
        String actualXml = FileUtils.fileRead( actual, "UTF-8" );
        XMLAssert.assertXMLEqual( expectedXml, actualXml );
    }

    @Before
    public void setUp()
    {
        lrm = new SplitLocalRepositoryManager( localRepoDir.getRoot() );
        session = new DefaultRepositorySystemSession();
        central = new RemoteRepository( "central", "default", "file:" );
    }

    @After
    public void tearDown()
    {
        lrm = null;
        session = null;
        central = null;
    }

    @Test
    public void testGetRepository()
    {
        LocalRepository repo = lrm.getRepository();
        assertNotNull( repo );
        assertEquals( localRepoDir.getRoot(), repo.getBasedir() );
        assertEquals( "splitted", repo.getContentType() );
    }

    @Test
    public void testGetPathForLocalArtifact()
    {
        String path = lrm.getPathForLocalArtifact( newArtifact( "g.i.d:aid:jar:jdk14:1.0" ) );
        assertEquals( LR + "g/i/d/aid/1.0/aid-1.0-jdk14.jar", path );

        path = lrm.getPathForLocalArtifact( newArtifact( "g.i.d:aid:jar:jdk14:1.0-SNAPSHOT" ) );
        assertEquals( LS + "g/i/d/aid/1.0-SNAPSHOT/aid-1.0-SNAPSHOT-jdk14.jar", path );

        path = lrm.getPathForLocalArtifact( newArtifact( "g.i.d:aid:jar:jdk14:1.0-20110914.193659-123" ) );
        assertEquals( LS + "g/i/d/aid/1.0-SNAPSHOT/aid-1.0-SNAPSHOT-jdk14.jar", path );
    }

    @Test
    public void testGetPathForRemoteArtifact()
    {
        String path = lrm.getPathForRemoteArtifact( newArtifact( "g.i.d:aid:jar:jdk14:1.0" ), central, "" );
        assertEquals( RR + "g/i/d/aid/1.0/aid-1.0-jdk14.jar", path );

        path = lrm.getPathForRemoteArtifact( newArtifact( "g.i.d:aid:jar:jdk14:1.0-SNAPSHOT" ), central, "" );
        assertEquals( RS + "g/i/d/aid/1.0-SNAPSHOT/aid-1.0-SNAPSHOT-jdk14.jar", path );

        path = lrm.getPathForRemoteArtifact( newArtifact( "g.i.d:aid:jar:jdk14:1.0-20110914.193659-123" ), central, "" );
        assertEquals( RS + "g/i/d/aid/1.0-SNAPSHOT/aid-1.0-20110914.193659-123-jdk14.jar", path );
    }

    @Test
    public void testGetPathForLocalMetadata()
    {
        String path = lrm.getPathForLocalMetadata( newMetadata( "g.i.d:aid:1.0" ) );
        assertEquals( LR + "g/i/d/aid/1.0/maven-metadata-local.xml", path );

        path = lrm.getPathForLocalMetadata( newMetadata( "g.i.d:aid:1.0-SNAPSHOT" ) );
        assertEquals( LS + "g/i/d/aid/1.0-SNAPSHOT/maven-metadata-local.xml", path );

        path = lrm.getPathForLocalMetadata( newMetadata( "g.i.d:aid" ) );
        assertEquals( LR + "g/i/d/aid/maven-metadata-local.xml", path );

        path = lrm.getPathForLocalMetadata( newMetadata( "g.i.d" ) );
        assertEquals( LR + "g/i/d/maven-metadata-local.xml", path );
    }

    @Test
    public void testGetPathForRemoteMetadata()
    {
        String path = lrm.getPathForRemoteMetadata( newMetadata( "g.i.d:aid:1.0" ), central, "" );
        assertEquals( RR + "g/i/d/aid/1.0/maven-metadata-central.xml", path );

        path = lrm.getPathForRemoteMetadata( newMetadata( "g.i.d:aid:1.0-SNAPSHOT" ), central, "" );
        assertEquals( RS + "g/i/d/aid/1.0-SNAPSHOT/maven-metadata-central.xml", path );

        path = lrm.getPathForRemoteMetadata( newMetadata( "g.i.d:aid" ), central, "" );
        assertEquals( RR + "g/i/d/aid/maven-metadata-central.xml", path );

        path = lrm.getPathForRemoteMetadata( newMetadata( "g.i.d" ), central, "" );
        assertEquals( RR + "g/i/d/maven-metadata-central.xml", path );
    }

    @Test
    public void testFindLocalSnapshotArtifact()
        throws Exception
    {
        Artifact artifact = newArtifact( "g.i.d:aid:1.0-SNAPSHOT" );

        LocalArtifactRequest request = new LocalArtifactRequest( artifact, null, "" );

        LocalArtifactResult result = lrm.find( session, request );
        assertNull( result.getFile() );
        assertFalse( result.isAvailable() );

        File file = new File( localRepoDir.getRoot(), lrm.getPathForLocalArtifact( artifact ) );
        create( file );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertTrue( result.isAvailable() );
        assertNull( result.getRepository() );
    }

    @Test
    public void testFindLocalReleaseArtifact()
        throws Exception
    {
        Artifact artifact = newArtifact( "g.i.d:aid:1.0" );

        LocalArtifactRequest request = new LocalArtifactRequest( artifact, null, "" );

        LocalArtifactResult result = lrm.find( session, request );
        assertNull( result.getFile() );
        assertFalse( result.isAvailable() );

        File file = new File( localRepoDir.getRoot(), lrm.getPathForLocalArtifact( artifact ) );
        create( file );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertTrue( result.isAvailable() );
        assertNull( result.getRepository() );
    }

    @Test
    public void testFindRemoteSnapshotArtifact()
        throws Exception
    {
        Artifact artifact = newArtifact( "g.i.d:aid:1.0-20110914.193659-123" );

        LocalArtifactRequest request = new LocalArtifactRequest( artifact, Arrays.asList( central ), "" );

        LocalArtifactResult result = lrm.find( session, request );
        assertNull( result.getFile() );
        assertFalse( result.isAvailable() );
        assertNull( result.getRepository() );

        File file = new File( localRepoDir.getRoot(), lrm.getPathForRemoteArtifact( artifact, central, "" ) );
        create( file );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isAvailable() );
        assertNull( result.getRepository() );

        lrm.add( session, new LocalArtifactRegistration( artifact, central, Arrays.asList( "" ) ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertTrue( result.isAvailable() );
        assertEquals( central, result.getRepository() );
    }

    @Test
    public void testFindRemoteReleaseArtifact()
        throws Exception
    {
        Artifact artifact = newArtifact( "g.i.d:aid:1.0" );

        LocalArtifactRequest request = new LocalArtifactRequest( artifact, Arrays.asList( central ), "" );

        LocalArtifactResult result = lrm.find( session, request );
        assertNull( result.getFile() );
        assertFalse( result.isAvailable() );
        assertNull( result.getRepository() );

        File file = new File( localRepoDir.getRoot(), lrm.getPathForRemoteArtifact( artifact, central, "" ) );
        create( file );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isAvailable() );
        assertNull( result.getRepository() );

        lrm.add( session, new LocalArtifactRegistration( artifact, central, Arrays.asList( "" ) ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertTrue( result.isAvailable() );
        assertEquals( central, result.getRepository() );
    }

    @Test
    public void testFindLocalSnapshotMetadata()
        throws Exception
    {
        Metadata metadata = newMetadata( "g.i.d:aid:1.0-SNAPSHOT" );

        LocalMetadataRequest request = new LocalMetadataRequest( metadata, null, "" );

        LocalMetadataResult result = lrm.find( session, request );
        assertNull( result.getFile() );
        assertFalse( result.isStale() );

        File file = new File( localRepoDir.getRoot(), lrm.getPathForLocalMetadata( metadata ) );
        create( file );

        lrm.add( session, new LocalMetadataRegistration( metadata ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isStale() );
    }

    @Test
    public void testFindLocalGidAidMetadata()
        throws Exception
    {
        Metadata metadata = newMetadata( "g.i.d:aid" );

        LocalMetadataRequest request = new LocalMetadataRequest( metadata, null, "" );

        LocalMetadataResult result = lrm.find( session, request );
        assertNull( result.getFile() );
        assertFalse( result.isStale() );

        File file = new File( localRepoDir.getRoot(), lrm.getPathForLocalMetadata( metadata ) );
        create( file, "maven-metadata-in1.xml" );

        lrm.add( session, new LocalMetadataRegistration( metadata ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isStale() );

        FileUtils.deleteDirectory( new File( localRepoDir.getRoot(), LS ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertTrue( result.isStale() );
        assertXmlEqual( "maven-metadata-out1a.xml", file );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isStale() );

        create( file, "maven-metadata-in1.xml" );

        lrm.add( session, new LocalMetadataRegistration( metadata ) );

        FileUtils.deleteDirectory( new File( localRepoDir.getRoot(), LR ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertTrue( result.isStale() );
        assertXmlEqual( "maven-metadata-out1b.xml", file );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isStale() );
    }

    @Test
    public void testFindRemoteSnapshotMetadata()
        throws Exception
    {
        Metadata metadata = newMetadata( "g.i.d:aid:1.0-SNAPSHOT" );

        LocalMetadataRequest request = new LocalMetadataRequest( metadata, central, "" );

        LocalMetadataResult result = lrm.find( session, request );
        assertNull( result.getFile() );
        assertFalse( result.isStale() );

        File file = new File( localRepoDir.getRoot(), lrm.getPathForRemoteMetadata( metadata, central, "" ) );
        create( file );

        lrm.add( session, new LocalMetadataRegistration( metadata, central, Arrays.asList( "" ) ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isStale() );
    }

    @Test
    public void testFindRemoteGidAidMetadata()
        throws Exception
    {
        Metadata metadata = newMetadata( "g.i.d:aid" );

        LocalMetadataRequest request = new LocalMetadataRequest( metadata, central, "" );

        LocalMetadataResult result = lrm.find( session, request );
        assertNull( result.getFile() );
        assertFalse( result.isStale() );

        File file = new File( localRepoDir.getRoot(), lrm.getPathForRemoteMetadata( metadata, central, "" ) );
        create( file, "maven-metadata-in1.xml" );

        lrm.add( session, new LocalMetadataRegistration( metadata, central, Arrays.asList( "" ) ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isStale() );

        FileUtils.deleteDirectory( new File( localRepoDir.getRoot(), RS ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertTrue( result.isStale() );
        assertXmlEqual( "maven-metadata-out1a.xml", file );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isStale() );

        create( file, "maven-metadata-in1.xml" );

        lrm.add( session, new LocalMetadataRegistration( metadata, central, Arrays.asList( "" ) ) );

        FileUtils.deleteDirectory( new File( localRepoDir.getRoot(), RR ) );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertTrue( result.isStale() );
        assertXmlEqual( "maven-metadata-out1b.xml", file );

        result = lrm.find( session, request );
        assertEquals( file, result.getFile() );
        assertFalse( result.isStale() );
    }

}
