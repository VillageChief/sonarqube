/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import Step from './Step';

/*::
type Props = {|
  currentUser: { login: string },
  finished: boolean,
  open: boolean,
  onContinue: (token: string) => void,
  onOpen: () => void,
  stepNumber: number
|};
*/

/*::
type State = {
  canUseExisting: boolean,
  existingToken: string,
  loading: boolean,
  selection: string,
  tokenName?: string,
  token?: string
};
*/

export default class CodeScanAnalysisStep extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    selection: 'analysis'
  };

  componentDidMount() {
    this.mounted = true;
    this.props.onFinish();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  canContinue = () => {
    return true;
  };

  navigateToAnalysisClick = (event /*: Event */) => {
    event.preventDefault();
    this.props.skipOnboarding().then(() => {
      window.location.href = '/organizations/' + this.props.organization + '/extension/developer/projects';
    });
  }

  renderForm = () => {
    return (
      <div className="big-spacer-top">
        <div className="boxed-group-inner">
          CodeScan Cloud provides an integrated analysis which allows you to run Salesforce analysis. Navigate to the Organization
          or individual Project`s `Project Analysis` Section under the Administration menu.
        </div>
        <div className="boxed-group-inner">
        <iframe src="https://player.vimeo.com/video/246881519?VQ=HD720&autoplay=1" width="640" height="348" frameBorder="0" webkitallowfullscreen="true" mozallowfullscreen="true" allowFullScreen="true" />
        </div>
        <div className="boxed-group-inner">
            <a className="button" href="#" onClick={this.navigateToAnalysisClick}>
              Go there now
            </a>
            <i className="icon-check spacer-right" />
        </div>
      </div>
    );
  };

  renderResult = () => {
    return (
      <div className="boxed-group-actions" />
    );
  };

  render() {
    return (
      <Step
        finished={this.props.finished}
        onOpen={this.props.onOpen}
        open={this.props.open}
        renderForm={this.renderForm}
        renderResult={this.renderResult}
        stepNumber={this.props.stepNumber}
        stepTitle="Create an analysis project"
      />
    );
  }
}
