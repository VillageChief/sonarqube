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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
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
  private static final Logger LOG = Loggers.get(CodeScanBranchConfigurationLoader.class);

  private BranchType convertBranchType(String branchType){
    if ( "short".equalsIgnoreCase(branchType) ) {
      return BranchType.SHORT;
    }else if ( "long".equalsIgnoreCase(branchType) ) {
      return BranchType.LONG;
    }else if ( branchType != null && !"".equals(branchType) ) {
      throw MessageException.of("'sonar.branch.type' is invalid. Must be short or long");
    }
    return null;
  }

  @Override
  public BranchConfiguration load(Map<String, String> localSettings, Supplier<Map<String, String>> remoteSettingsSupplier, ProjectBranches branches, ProjectPullRequests pullRequests) {
    String branchName = StringUtils.trimToNull((String) localSettings.get("sonar.branch.name"));
    String branchTarget = StringUtils.trimToNull(localSettings.get("sonar.branch.target"));
    String targetScmBranch = branchTarget;
    BranchType branchType = convertBranchType(localSettings.get("sonar.branch.type"));

    //no branch config. Use default settings
    if (branchName == null && branchTarget == null ) {
      return new DefaultBranchConfiguration();
    }

    //else we are using branch feature... always need a branch name
    if (branchName == null) {
      throw MessageException.of("'sonar.branch.name' is required for a branch analysis");
    }

    //basic sanity checks
    if ( branchTarget != null && branchName.equals(branchTarget)) {
      throw MessageException.of("'sonar.branch.name' cannot be the same as the 'sonar.branch.target'");
    }

    //infer branch target.
    if ( branchTarget == null ) {
      String defaultBranchName = branches.defaultBranchName();
      //use default branch
      if (defaultBranchName != null && !branchName.equals(defaultBranchName)) {
        branchTarget = defaultBranchName;
        LOG.debug("missing sonar.branch.target set to {}", branchTarget);
      }
    }

    //fetch existing target
    if ( branchTarget != null ) {
      BranchInfo branchTargetInfo = getBranchInfo(branches, branchTarget);

      if ( branchTargetInfo.type() == BranchType.SHORT ) {
        //if we're merging into a non-master type branch...
        if ( branchTargetInfo.branchTargetName() == null ) {
          //we need to have a target for that one
          throw MessageException.of("Target branch is short-lived and has a short-lived branch as a target: " + branchTarget);
        } else {
          //use the parent of the target.
          branchTarget = branchTargetInfo.branchTargetName();
          LOG.debug("sonar.branch.target set to parent of target: {}", branchTarget);
        }
      }
    }else if ( branches.defaultBranchName() == null && !branchName.equals("master") ){
      throw MessageException.of("First time run! Please run main branch on master for the first time");
    }else if ( branches.defaultBranchName() == null && branchName.equals("master") ){
      return new DefaultBranchConfiguration();
    }

    /**
     * The long living server branch from which we should load project settings/quality profiles/compare changed files/...
     * For long living branches, this is the sonar.branch.target (default to default branch) in case of first analysis,
     * otherwise it's the branch itself.
     * For short living branches, we look at sonar.branch.target (default to default branch). If it exists but is a short living branch or PR, we will
     * transitively use its own target.
     */
    String longLivingSonarReferenceBranch = branchTarget;

    //get info of existing branch
    BranchInfo branchInfo = branches.get(branchName);
    if (branchInfo != null) {

      //can't merge main branch onto something else
      if ( branchTarget != null && branchInfo.isMain()) {
        throw MessageException.of("Cannot pass a branch target to the main branch");
      }

      //check that branch type doesn't cahnge
      if ( branchType != null && branchType != branchInfo.type() ){
        throw MessageException.of("Cannot change branch type as branch already exists");
      }else if ( branchType == null ){
        branchType = branchInfo.type();
        LOG.debug("sonar.branch.type set to existing type of: {}", branchType);
      }

      //see above
      if (branchType == BranchType.LONG) {
        longLivingSonarReferenceBranch = branchName;
      }

    }else if ( branchType == null && branchTarget == null ) {
      branchType = BranchType.LONG;
      LOG.debug("sonar.branch.type set: {}", branchType);
    }else if ( branchType == null ) {
      //figure out branch type based on defaults
      String regex = (String)remoteSettingsSupplier.get().get("sonar.branch.longLivedBranches.regex");
      if (regex == null) {
        regex = "(branch|release)-.*";
      }
      branchType = branchName.matches(regex) ? BranchType.LONG : BranchType.SHORT;
      LOG.debug("sonar.branch.type set based on regex to: {}", branchType);
    }
    return new CodeScanBranchConfiguration(branchType, branchName, branchTarget, targetScmBranch, longLivingSonarReferenceBranch);
  }

  private static BranchInfo getBranchInfo(ProjectBranches branches, String branchTarget) {
    BranchInfo ret = branches.get(branchTarget);
    if (ret == null) {
      throw MessageException.of("Target branch does not exist on server. Run a regular analysis before running a branch analysis");
    } else {
      return ret;
    }
  }
}
