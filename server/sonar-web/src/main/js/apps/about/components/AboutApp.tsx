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
import { connect } from 'react-redux';
import { Location } from 'history';
import AboutProjects from './AboutProjects';
import AboutLanguages from './AboutLanguages';
import AboutCleanCode from './AboutCleanCode';
import AboutQualityModel from './AboutQualityModel';
import AboutQualityGates from './AboutQualityGates';
import AboutLeakPeriod from './AboutLeakPeriod';
import AboutStandards from './AboutStandards';
import AboutScanners from './AboutScanners';
import EntryIssueTypes from './EntryIssueTypes';
import GlobalContainer from '../../../app/components/GlobalContainer';
import { searchProjects } from '../../../api/components';
import { getFacet } from '../../../api/issues';
import { fetchAboutPageSettings } from '../actions';
import {
  getAppState,
  getCurrentUser,
  getGlobalSettingValue,
  Store
} from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import { addWhitePageClass, removeWhitePageClass } from '../../../helpers/pages';
import '../styles.css';

interface Props {
  appState: Pick<T.AppState, 'defaultOrganization' | 'organizationsEnabled'>;
  currentUser: T.CurrentUser;
  customText?: string;
  fetchAboutPageSettings: () => Promise<void>;
  location: Location;
}

interface State {
  issueTypes?: { [key: string]: { count: number } };
  loading: boolean;
}

class AboutApp extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: true,
  };

  componentDidMount() {
    this.mounted = true;
    window.location.href = 'https://www.codescan.io/cloud/';
    document.body.classList.add('white-page');
    if (document.documentElement) {
      document.documentElement.classList.add('white-page');
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    removeWhitePageClass();
  }

  render() {
    return (<span></span>)
  }
}

const mapStateToProps = (state: Store) => {
  const customText = getGlobalSettingValue(state, 'sonar.lf.aboutText');
  return {
    appState: getAppState(state),
    currentUser: getCurrentUser(state),
    customText: customText && customText.value
  };
};

const mapDispatchToProps = { fetchAboutPageSettings } as any;

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(AboutApp);
