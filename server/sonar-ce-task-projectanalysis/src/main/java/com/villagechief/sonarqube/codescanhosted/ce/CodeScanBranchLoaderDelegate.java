/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.villagechief.sonarqube.codescanhosted.ce;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.BranchLoaderDelegate;
import org.sonar.db.component.BranchType;
import org.sonar.server.project.Project;

/**
 * CE calls this as a delegate in the BranchLoader. Set a sane branch name..
 * @author ben
 *
 */
public class CodeScanBranchLoaderDelegate implements BranchLoaderDelegate {
	private final DbClient dbClient;
	private final MutableAnalysisMetadataHolder metadataHolder;
	
	public CodeScanBranchLoaderDelegate(DbClient dbClient, MutableAnalysisMetadataHolder metadataHolder) {
		this.dbClient = dbClient;
		this.metadataHolder = metadataHolder;
	}

    public void load(final ScannerReport.Metadata metadata) {
        this.metadataHolder.setBranch(doload(metadata));
    }
	private CodeScanBranch doload(final ScannerReport.Metadata metadata ){
        String targetBranch = StringUtils.trimToNull(metadata.getMergeBranchName());

        //get the branch name or default to master
        String branchName = StringUtils.trimToNull( metadata.getBranchName() );
        if ( branchName == null ){
            String projectUuid = StringUtils.trimToNull(this.metadataHolder.getProject().getUuid());
            Optional<BranchDto> branchDto = findBranchByProjectUuid(projectUuid);
            if (branchDto.isPresent()) {
                BranchDto dto = branchDto.get();
                return new CodeScanBranch(dto.getKey(), null, dto.getBranchType(), dto.isMain());
            } else {
                throw new IllegalStateException("Could not find main branch");
            }
        }
        
        //find an existing target branch
        boolean isMain = false;
        BranchType branchType = this.convertBranchType(metadata.getBranchType());
        final Optional<BranchDto> branchDto = this.findBranchByKey(branchName);
        if (branchDto.isPresent()) {
        	//check that the branch type hasn't changed....
        	if (branchDto.get().getBranchType() != branchType) {
                throw new IllegalStateException(String.format( "Branch '%s' already exists with type '%s', type '%s' not possible.", branchName, branchDto.get().getBranchType(), branchType ));
            }
        	//set the isMain based on existing...
            isMain = branchDto.get().isMain();
        }
        
        //find the target branch...
        String targetBranchUuid = null;
        if ( targetBranch != null ) {
        	targetBranchUuid = this.findTargetBranchUuid(targetBranch);
        }else if ( branchType == BranchType.SHORT ){
        	throw new IllegalArgumentException(String.format("Branch target for '%s' not set, so branch type should be LONG but is '%s'", branchName, branchType));
        }
        //set the output branch...
        return new CodeScanBranch(branchName, targetBranchUuid, branchType, isMain);
    }
    
    private BranchType convertBranchType(final ScannerReport.Metadata.BranchType branchType) {
        switch ( branchType.ordinal() ) {
	        case ScannerReport.Metadata.BranchType.LONG_VALUE:
	        case ScannerReport.Metadata.BranchType.UNSET_VALUE:
	            return BranchType.LONG;
            case ScannerReport.Metadata.BranchType.SHORT_VALUE:
                return BranchType.SHORT;
            default:
                throw new IllegalStateException( "Invalid branch type: " + branchType);
        }
    }
    
    private String findTargetBranchUuid(final String branchName) {
        final Project project = this.metadataHolder.getProject();
        final Optional<BranchDto> branch = this.findBranchByKey(project.getUuid(), branchName);
        if ( branch.isPresent() ) {
            final BranchDto branchDto = branch.get();
            if ( branchDto.getBranchType() == BranchType.LONG ) {
            	return branchDto.getUuid();
            }else {
            	throw new IllegalArgumentException("Target branch '" + branchName + "' should be LONG but is '" + branchDto.getBranchType() + "'");
            }
        }else {
        	throw new IllegalArgumentException("Target Branch '" + branchName + "' does not exist");
        }
    }

    private Optional<BranchDto> findBranchByKey(final String branchName) {
        final Project project = this.metadataHolder.getProject();
        return this.findBranchByKey(project.getUuid(), branchName);
    }


    private Optional<BranchDto> findBranchByKey(final String projectUuid, final String key) {
        try (final DbSession openSession = this.dbClient.openSession(false)){
            return this.dbClient.branchDao().selectByBranchKey(openSession, projectUuid, key);
        }
    }

    private Optional<BranchDto> findBranchByProjectUuid(final String projectUuid) {
        try (final DbSession openSession = this.dbClient.openSession(false)){
            return this.dbClient.branchDao().selectByUuid(openSession, projectUuid);
        }
    }
}
