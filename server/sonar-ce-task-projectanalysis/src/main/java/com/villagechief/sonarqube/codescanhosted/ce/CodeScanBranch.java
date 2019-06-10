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
import org.picocontainer.annotations.Nullable;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

/** Dto for the branch name */
class CodeScanBranch implements Branch
{
    private final String branchName;
    private final String targetBranchUuid;
    private final BranchType branchType;
    private final boolean isMain;

    CodeScanBranch(final String branchName, @Nullable final String targetBranchUuid, final BranchType branchType, final boolean isMain) {
        this.branchName = branchName;
        this.targetBranchUuid = targetBranchUuid;
        this.branchType = branchType;
        this.isMain = isMain;
        
        //kee is 255 long...
        if ( branchName.length() > 255) {
            throw MessageException.of(String.format("Illegal branch name: '%s'. Branch name must be less than 255 characters.", branchName ));
        }
        
        //:BRANCH: is used to separate the project key, so it can't contain it
        if (StringUtils.contains(branchName, ":BRANCH:")) {
            throw MessageException.of(String.format("Illegal branch name: '%s'. Branch name cannot contain ':BRANCH:'.", branchName ));
        }
    }
    
    public BranchType getType() {
        return this.branchType;
    }
    
    public Optional<String> getMergeBranchUuid() {
        return Optional.ofNullable(this.targetBranchUuid);
    }
    
    public boolean isMain() {
        return this.isMain;
    }
    
    public boolean isLegacyFeature() {
        return false;
    }
    
    public String getName() {
        return this.branchName;
    }
    
    public boolean supportsCrossProjectCpd() {
        return this.isMain;
    }

    public String generateKey(String projectKey, @javax.annotation.Nullable String fileOrDirPath){
        String key;
        if (isEmpty(fileOrDirPath)) {
            key = projectKey;
        }else {
            key = ComponentKeys.createEffectiveKey(projectKey, trimToNull(fileOrDirPath));
        }
        return generateKey(key);

    }
    public String generateKey(String projectKey){
        if (this.isMain) {
            return projectKey;
        }
        return ComponentDto.generateBranchKey(projectKey, this.branchName);
    }

    @Override
    public String getPullRequestKey() {
      return null;
    }

    public String toString(){
        return "CodeScanBranch{branchName=" + branchName + ", targetBranchUuid=" + targetBranchUuid + ",branchType=" + branchType + ",isMain=" + isMain + '}';
    }

}
