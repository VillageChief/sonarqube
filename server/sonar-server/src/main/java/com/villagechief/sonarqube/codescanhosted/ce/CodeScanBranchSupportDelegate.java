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

import org.sonar.server.ce.queue.BranchSupportDelegate;
import org.sonar.server.ce.queue.BranchSupport.Branch;
import org.sonar.server.ce.queue.BranchSupport.ComponentKey;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;

/**
 * CE calls this as a delegate in the BranchLoader. Set a sane branch name..
 * @author ben
 *
 */
public class CodeScanBranchSupportDelegate implements BranchSupportDelegate {
	private final UuidFactory uuidFactory;
	private final System2 system2;
	private final DbClient dbClient;

	public CodeScanBranchSupportDelegate(UuidFactory uuidFactory, System2 system2, DbClient dbClient) {
		this.uuidFactory = uuidFactory;
		this.system2 = system2;
		this.dbClient = dbClient;
	}

	public ComponentKey createComponentKey(String projectKey, Map<String, String> characteristics) {
		String branchTypeString = StringUtils.trimToNull((String)characteristics.get("branchType"));
		String branchName = StringUtils.trimToNull((String)characteristics.get("branch"));
		BranchType branchType = toBranchType(branchTypeString);
		return new BranchComponent(projectKey, ComponentDto.generateBranchKey(projectKey, branchName),
				new Branch(branchName, branchType));
	}

	private static BranchType toBranchType(String branchType) {
		if (branchType.equalsIgnoreCase("SHORT")) {
			return BranchType.SHORT;
		} else if (branchType.equalsIgnoreCase("LONG")) {
			return BranchType.LONG;
		} else {
			throw new IllegalArgumentException("Unsupported branch type " + branchType);
		}
	}

	public ComponentDto createBranchComponent(DbSession dbSession, ComponentKey componentKey,
											  OrganizationDto organization, ComponentDto mainComponentDto, BranchDto mainComponentBranchDto) {
		if (componentKey.getBranch().filter((x) -> {
			return x.getName().equals(mainComponentBranchDto.getKey()) && x.getType() == mainComponentBranchDto.getBranchType();
		}).isPresent()) {
			return mainComponentDto;
		} else {
			String uuid = this.uuidFactory.create();
			ComponentDto ret = mainComponentDto.copy();
			ret
				.setUuidPath(".")
				.setUuid(uuid)
				.setProjectUuid(uuid)
				.setRootUuid(uuid)
				.setDbKey(componentKey.getDbKey())
				.setCreatedAt(new Date(this.system2.now()))
				.setModuleUuidPath("." + uuid + ".")
				.setMainBranchProjectUuid(mainComponentDto.uuid());
			this.dbClient.componentDao().insert(dbSession, ret);
			return ret;
		}
	}

	private static final class BranchComponent extends ComponentKey {
		private final String projectKey;
		private final String dbKey;
		@CheckForNull
		private final Branch branch;

		private BranchComponent(String projectKey, String dbKey, @CheckForNull Branch branch) {
			this.projectKey = projectKey;
			this.dbKey = dbKey;
			this.branch = branch;
		}

		public String getKey() {
			return this.projectKey;
		}

		public String getDbKey() {
			return this.dbKey;
		}

		public Optional<String> getDeprecatedBranchName() {
			return Optional.empty();
		}

		public Optional<Branch> getBranch() {
			return Optional.ofNullable(this.branch);
		}

		public Optional<String> getPullRequestKey() {
			return Optional.empty();
		}

		public ComponentKey getMainBranchComponentKey() {
			return this.projectKey.equals(this.dbKey) ?
					this : new BranchComponent(this.projectKey, this.projectKey, (Branch)null);
		}

		public boolean equals(Object other) {
			if (this == other) {
				return true;
			} else if (other != null && this.getClass() == other.getClass()) {
				BranchComponent that = (BranchComponent)other;
				return Objects.equals(this.projectKey, that.projectKey)
						&& Objects.equals(this.dbKey, that.dbKey)
						&& Objects.equals(this.branch, that.branch) ;
			} else {
				return false;
			}
		}

		public int hashCode() {
			return Objects.hash(new Object[]{this.projectKey, this.dbKey, this.branch});
		}

		public String toString() {
			return "BranchComponent{key='" + this.projectKey + '\'' + ", dbKey='" + this.dbKey + '\'' + ", branch=" + this.branch + '}';
		}
	}
}
