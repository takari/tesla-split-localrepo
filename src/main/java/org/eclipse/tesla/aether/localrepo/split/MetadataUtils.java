package org.eclipse.tesla.aether.localrepo.split;

/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.metadata.Metadata;

/**
 */
class MetadataUtils
{

    static final String MAVEN_METADATA = "maven-metadata.xml";

    public static void repair( Metadata metadata, File src, File dst, boolean purgeSnapshots, boolean purgeReleases )
        throws IOException
    {
        if ( MAVEN_METADATA.equals( metadata.getType() ) && metadata.getArtifactId().length() > 0
            && metadata.getVersion().length() <= 0 )
        {
            Xpp3Dom dom = readXml( src );

            Xpp3Dom versioning = dom.getChild( "versioning" );
            if ( versioning != null )
            {
                Xpp3Dom release = versioning.getChild( "release" );
                Xpp3Dom latest = versioning.getChild( "latest" );
                if ( purgeSnapshots && latest != null && release != null && latest.getValue() != null
                    && latest.getValue().endsWith( "SNAPSHOT" ) )
                {
                    latest.setValue( release.getValue() );
                }
                if ( purgeReleases && release != null && release.getValue() != null )
                {
                    release.setValue( null );
                }

                Xpp3Dom versions = versioning.getChild( "versions" );
                if ( versions != null )
                {
                    for ( int i = versions.getChildCount() - 1; i >= 0; i-- )
                    {
                        Xpp3Dom child = versions.getChild( i );
                        if ( !"version".equals( child.getName() ) || child.getValue() == null )
                        {
                            continue;
                        }
                        String version = child.getValue();
                        if ( version.endsWith( "SNAPSHOT" ) )
                        {
                            if ( purgeSnapshots )
                            {
                                versions.removeChild( i );
                            }
                        }
                        else
                        {
                            if ( purgeReleases )
                            {
                                versions.removeChild( i );
                            }
                        }
                    }
                }
            }

            writeXml( dst, dom );
        }
        else
        {
            FileUtils.copyFile( src, dst );
        }
    }

    private static Xpp3Dom readXml( File src )
        throws IOException
    {
        try ( Reader reader = ReaderFactory.newXmlReader( src ) )
        {
            return Xpp3DomBuilder.build( reader, false );
        }
        catch ( XmlPullParserException e )
        {
            throw (IOException) new IOException( e.getMessage() ).initCause( e );
        }
    }

    private static void writeXml( File dst, Xpp3Dom dom )
        throws IOException
    {
        dst.getAbsoluteFile().getParentFile().mkdirs();

        Writer writer = WriterFactory.newXmlWriter( dst );
        try
        {
            Xpp3DomWriter.write( writer, dom );
        }
        finally
        {
            writer.close();
        }
    }

}
