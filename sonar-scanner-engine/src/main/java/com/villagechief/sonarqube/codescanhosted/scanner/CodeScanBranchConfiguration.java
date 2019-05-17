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

import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;

import javax.annotation.CheckForNull;

/** 
 * The branch configuration that we return from the loader
 * @author ben
 *
 */
public class CodeScanBranchConfiguration implements BranchConfiguration {
	private final BranchType branchType;
	private final String branchName;
	private final String branchTarget;
	private final String branchBase;

	public CodeScanBranchConfiguration(BranchType branchType, String branchName, String branchTarget, String branchBase) {
		this.branchType = branchType;
		this.branchName = branchName;
		this.branchTarget = branchTarget;
		this.branchBase = branchBase;
	}
	
	@Override
	public BranchType branchType() {
		return branchType;
	}

	@Override
	public String branchName() {
		return branchName;
	}

	@CheckForNull
	@Override
	public String longLivingSonarReferenceBranch() {
		return branchBase;
	}

	@CheckForNull
	@Override
	public String targetScmBranch() {
		return branchTarget;
	}

	@Override
	public String pullRequestKey() {
	  return null;
	}


}
