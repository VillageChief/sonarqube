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
package com.villagechief.sonarqube.codescanhosted.scanner;

import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchConfigurationLoader;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.DefaultBranchConfiguration;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectPullRequests;

/**
 * This class is a loader to return a branch configuration to use for the
 * current build...
 * 
 * @author ben
 *
 */
public class CodeScanBranchConfigurationLoader implements BranchConfigurationLoader {

  @Override
  public BranchConfiguration load(Map<String, String> localSettings, Supplier<Map<String, String>> remoteSettingsSupplier, ProjectBranches branches, ProjectPullRequests pullRequests) {
    String branchName = StringUtils.trimToNull((String) localSettings.get("sonar.branch.name"));
    String branchTarget = StringUtils.trimToNull(localSettings.get("sonar.branch.target"));
    BranchType branchType = "short".equals(localSettings.get("sonar.branch.type")) ? BranchType.SHORT : BranchType.LONG;
    String branchBase = null;

    if (branchName == null && branchTarget == null ) {
      return new DefaultBranchConfiguration();
    }else{
      //resolve target if necessary
      branchBase = branchTarget;
      
      if (branchName == null) {
        throw MessageException.of("'sonar.branch.name' is required for a branch analysis");
      }

      //set branch target...
      if (branches.isEmpty()) {
        throw MessageException.of("No branches found... Have you run a non-branch analysis yet?");
      }
      
      //get info of existing branch
      BranchInfo branchInfo = branches.get(branchName);
      if (branchInfo != null) {
        //can't merge main branch onto something else
//        if (branchInfo.isMain()) {
//          throw MessageException.of("Cannot pass a branch target to the main branch");
//        }
  
        branchType = branchInfo.type();
        if (branchType == BranchType.LONG) {
          branchBase = branchName;
        }
      }//else we could convert branchType based on the name or something
  
      if ( branchTarget != null ) {
        BranchInfo branchTargetInfo = getBranchInfo(branches, branchTarget);
        if ( branchTargetInfo.type() != BranchType.LONG ) {
          //if we're merging into a non-master type branch...
          if ( branchTargetInfo.branchTargetName() == null ) {
            //we need to have a target for that one
            throw MessageException.of("Target branch is short-lived and has a short-lived branch as a target: " + branchTarget);
          } else {
            //use the parent of the target.
            branchTarget = branchTargetInfo.name();
          }
        }
      }else{
        String defaultBranchName = branches.defaultBranchName();
        if (!branchName.equals(defaultBranchName)) {
          branchTarget = defaultBranchName;
        }
      }

      return new CodeScanBranchConfiguration(branchType, branchName, branchTarget, branchBase);
    }
  }

  private static BranchInfo getBranchInfo(ProjectBranches branches, String branchTarget) {
    BranchInfo ret = branches.get(branchTarget);
    if (ret == null) {
      throw MessageException.of("Target branch does not exist on server: " + branchTarget);
    } else {
      return ret;
    }
  }
}
