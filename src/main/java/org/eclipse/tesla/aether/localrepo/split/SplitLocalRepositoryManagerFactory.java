package org.eclipse.tesla.aether.localrepo.split;

/*******************************************************************************
 * Copyright (c) 2011, 2021 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;

/**
 * Creates local repository managers for the local repository types {@code "split"} and {@code ""} (automatic).
 */
@Component( role = LocalRepositoryManagerFactory.class, hint = "split" )
public class SplitLocalRepositoryManagerFactory
    implements LocalRepositoryManagerFactory
{

    @Override
    public LocalRepositoryManager newInstance( RepositorySystemSession session, LocalRepository repository )
        throws NoLocalRepositoryManagerException
    {
        if ( "".equals( repository.getContentType() ) || "split".equals( repository.getContentType() ) )
        {
            return new SplitLocalRepositoryManager( repository.getBasedir() );
        }
        else
        {
            throw new NoLocalRepositoryManagerException( repository );
        }
    }

    public float getPriority()
    {
        return 50f;
    }

}
