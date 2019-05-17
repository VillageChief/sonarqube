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
import * as React from 'react';
import OrganizationInput from './OrganizationInput';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { SubmitButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import { isSonarCloud } from '../../../helpers/system';
import { getBaseUrl } from '../../../helpers/urls';
import './ManualProjectCreate.css';

interface Props {
  currentUser: T.LoggedInUser;
  fetchMyOrganizations?: () => Promise<void>;
  onProjectCreate: (projectKeys: string[]) => void;
  organization?: string;
  userOrganizations?: T.Organization[];
}

interface State {
  selectedOrganization?: T.Organization;
  submitting: boolean;
}

export default class ManualProjectCreate extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      selectedOrganization: this.getInitialSelectedOrganization(props),
      submitting: false
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  canSubmit(state: State) {
    return Boolean(
      state.selectedOrganization
    );
  }

  getInitialSelectedOrganization = (props: Props) => {
    if (props.organization) {
      return this.getOrganization(props.organization);
    } else if (props.userOrganizations && props.userOrganizations.length === 1) {
      return props.userOrganizations[0];
    } else {
      return undefined;
    }
  };

  getOrganization = (organizationKey: string) => {
    return (
      this.props.userOrganizations &&
      this.props.userOrganizations.find(({ key }: T.Organization) => key === organizationKey)
    );
  };

  handleFormSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { state } = this;
    if (this.canSubmit(state)) {
      this.setState({ submitting: true });
      window.location.href =
        getBaseUrl() + `/organizations/${this.state.selectedOrganization.key}/extension/developer/projects`;
    }
  };

  handleOrganizationSelect = ({ key }: T.Organization) => {
    const selectedOrganization = this.getOrganization(key);
    this.setState({
      selectedOrganization
    });
  };

  render() {
    const { selectedOrganization, submitting } = this.state;

    return (
      <div className="create-project">
        <div className="flex-1 huge-spacer-right">
          <form className="manual-project-create" onSubmit={this.handleFormSubmit}>
            {isSonarCloud() &&
              this.props.userOrganizations && (
                <OrganizationInput
                  onChange={this.handleOrganizationSelect}
                  organization={selectedOrganization ? selectedOrganization.key : ''}
                  organizations={this.props.userOrganizations}
                />
              )}
            <SubmitButton disabled={!this.canSubmit(this.state) || submitting}>
              {translate('set_up')}
            </SubmitButton>
            <DeferredSpinner className="spacer-left" loading={submitting} />
          </form>
        </div>
      </div>
    );
  }
}
