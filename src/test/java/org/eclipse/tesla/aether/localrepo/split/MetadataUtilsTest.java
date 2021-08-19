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

import org.codehaus.plexus.util.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.metadata.DefaultMetadata;

/**
 */
public class MetadataUtilsTest
{

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private File getSrc( String name )
    {
        return new File( "src/test/resources/metadata/" + name ).getAbsoluteFile();
    }

    private void assertXmlEqual( String expectedName, File actual )
        throws Exception
    {
        File expected = getSrc( expectedName );

        String expectedXml = FileUtils.fileRead( expected, "UTF-8" );
        String actualXml = FileUtils.fileRead( actual, "UTF-8" );
        XMLAssert.assertXMLEqual( expectedXml, actualXml );
    }

    @Test
    public void testRepairPurgeSnapshots()
        throws Exception
    {
        File src = getSrc( "maven-metadata-in1.xml" );
        File dst = new File( tempDir.getRoot(), "sub/dir/" + src.getName() );

        Metadata md =
            new DefaultMetadata( "gid", "aid", MetadataUtils.MAVEN_METADATA, Metadata.Nature.RELEASE_OR_SNAPSHOT );

        MetadataUtils.repair( md, src, dst, true, false );

        assertXmlEqual( "maven-metadata-out1a.xml", dst );
    }

    @Test
    public void testRepairPurgeReleases()
        throws Exception
    {
        File src = getSrc( "maven-metadata-in1.xml" );
        File dst = new File( tempDir.getRoot(), "sub/dir/" + src.getName() );

        Metadata md =
            new DefaultMetadata( "gid", "aid", MetadataUtils.MAVEN_METADATA, Metadata.Nature.RELEASE_OR_SNAPSHOT );

        MetadataUtils.repair( md, src, dst, false, true );

        assertXmlEqual( "maven-metadata-out1b.xml", dst );
    }

    @Test
    public void testRepairPlainCopyOfUnsupportedMetadata()
        throws Exception
    {
        File src = getSrc( "maven-metadata-in2.xml" );
        File dst = new File( tempDir.getRoot(), "sub/dir/" + src.getName() );

        Metadata md = new DefaultMetadata( "gid", MetadataUtils.MAVEN_METADATA, Metadata.Nature.RELEASE_OR_SNAPSHOT );

        MetadataUtils.repair( md, src, dst, true, false );

        assertXmlEqual( src.getName(), dst );
    }

    @Test
    public void testRepairPlainCopyOfUnknownMetadata()
        throws Exception
    {
        File src = getSrc( "unknown-metadata-in1.xml" );
        File dst = new File( tempDir.getRoot(), "sub/dir/" + src.getName() );

        Metadata md = new DefaultMetadata( "gid", "aid", "unknown", Metadata.Nature.RELEASE_OR_SNAPSHOT );

        MetadataUtils.repair( md, src, dst, true, false );

        assertEquals( FileUtils.fileRead( src, "UTF-8" ), FileUtils.fileRead( dst, "UTF-8" ) );
    }

}
