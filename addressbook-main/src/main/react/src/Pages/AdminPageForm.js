import React from "react";
import { connect } from "react-redux";
import { bindActionCreators } from "redux";
import * as MenuActions from "./MenuFormActions";
import { Breadcrumb, Button, Label, Nav, Navbar, Table } from "react-bootstrap";
import { HashUtils } from "../Common/Utils";
import { UserComponent } from "../Components/UserComponent";

@connect(
  (state) => ({
    breadcrumbs: state.menuReducer.breadcrumbs,
  }),
  (dispatch) => ({
    getBreadcrumbs: bindActionCreators(MenuActions.getBreadcrumbs, dispatch),
    logout: bindActionCreators(MenuActions.logout, dispatch),
  })
)
export default class AdminPageForm extends React.Component {
  state = {
    jvmState: {},
    igniteState: {
      igniteCacheMetricsMap: {},
    },
  };

  constructor(props) {
    super(props);
  }

  onOpen = () => {
    console.log("Connection opened for JVM state");
  };

  onIgniteOpen = () => {
    console.log("Connection opened for Ignite state");
  };

  onError = () => {
    if (this.state.eventSource.readyState === EventSource.CONNECTING) {
      console.log("Reconnecting to JVM event source");
    } else {
      console.log("Error: " + this.state.eventSource.readyState);
    }
  };

  onIgniteError = () => {
    if (this.state.igniteEventSource.readyState === EventSource.CONNECTING) {
      console.log("Reconnecting to Ignite event source");
    } else {
      console.log("Error: " + this.state.igniteEventSource.readyState);
    }
  };

  onMessage = (e) => {
    let result = JSON.parse(e.data);
    this.setState({ jvmState: result });
  };

  onIgniteMessage = (e) => {
    let result = JSON.parse(e.data);
    this.setState({ igniteState: result });
  };

  componentDidMount() {
    let currentUrl = window.location.hash;
    this.props.getBreadcrumbs(HashUtils.cleanHash(currentUrl));

    let newEventSource = new EventSource("/rest/admin/jvmState");
    newEventSource.onopen = this.onOpen;
    newEventSource.onmessage = this.onMessage;
    newEventSource.onerror = this.onError;

    let newIgniteEventSource = new EventSource("/rest/admin/igniteState");
    newIgniteEventSource.onopen = this.onIgniteOpen;
    newIgniteEventSource.onmessage = this.onIgniteMessage;
    newIgniteEventSource.onerror = this.onIgniteError;

    this.setState({
      eventSource: newEventSource,
      igniteEventSource: newIgniteEventSource,
    });
  }

  componentWillUnmount() {
    this.state.eventSource.close();
    this.state.igniteEventSource.close();
  }

  render() {
    let igniteMetrics = [];
    let allMetrics = this.state.igniteState.igniteCacheMetricsMap;
    for (const key of Object.keys(allMetrics)) {
      const cache = allMetrics[key];
      igniteMetrics.push(
        <div>
          <h4>
            <Label bsStyle="danger">{key}</Label>
          </h4>
          <Table striped bordered condensed hover>
            <tr>
              <td>Cache gets</td>
              <td>{cache.cacheGets}</td>
            </tr>
            <tr>
              <td>Cache puts</td>
              <td>{cache.cachePuts}</td>
            </tr>
            <tr>
              <td>Cache removals</td>
              <td>{cache.cacheRemovals}</td>
            </tr>
            <tr>
              <td>Average get time</td>
              <td>{cache.averageGetTime}</td>
            </tr>
            <tr>
              <td>Average put time</td>
              <td>{cache.averagePutTime}</td>
            </tr>
            <tr>
              <td>Average remove time</td>
              <td>{cache.averageRemoveTime}</td>
            </tr>
            <tr>
              <td>Off-heap gets</td>
              <td>{cache.offHeapGets}</td>
            </tr>
            <tr>
              <td>Off-heap puts</td>
              <td>{cache.offHeapPuts}</td>
            </tr>
            <tr>
              <td>Off-heap removals</td>
              <td>{cache.offHeapRemovals}</td>
            </tr>
            <tr>
              <td>Heap entries count</td>
              <td>{cache.heapEntriesCount}</td>
            </tr>
            <tr>
              <td>Off-heap entries count</td>
              <td>{cache.offHeapEntriesCount}</td>
            </tr>
          </Table>
        </div>
      );
    }

    let breads = [];
    let breadcrumbsCount = this.props.breadcrumbs.length;
    this.props.breadcrumbs.forEach(function (element, index) {
      breads.push(
        <Breadcrumb.Item
          style={{
            fontWeight: index === breadcrumbsCount - 1 ? "bold" : "normal",
          }}
          key={element.url}
          href={"#" + element.url}
        >
          {" "}
          {element.name}{" "}
        </Breadcrumb.Item>
      );
    });
    return (
      <div>
        <Navbar>
          <Navbar.Collapse>
            <Nav>
              <Breadcrumb>{breads}</Breadcrumb>
            </Nav>
            <Nav pullRight>
              <UserComponent />
              <Button onClick={() => this.props.logout()}>Logout</Button>
            </Nav>
          </Navbar.Collapse>
        </Navbar>
        <div id="adminContainer">
          <h4>
            <Label bsStyle="danger">Runtime</Label>
          </h4>
          <Table striped bordered condensed hover>
            <tr>
              <td>Total memory</td>
              <td>{this.state.jvmState.runtimeTotalMemory}</td>
            </tr>
            <tr>
              <td>Free memory</td>
              <td>{this.state.jvmState.runtimeFreeMemory}</td>
            </tr>
            <tr>
              <td>Max memory</td>
              <td>{this.state.jvmState.runtimeMaxMemory}</td>
            </tr>
          </Table>
          <h4>
            <Label bsStyle="danger">System</Label>
          </h4>
          <Table striped bordered condensed hover>
            <tr>
              <td>Available processors</td>
              <td>{this.state.jvmState.availableProcessors}</td>
            </tr>
            <tr>
              <td>Load average</td>
              <td>{this.state.jvmState.systemLoadAverage}</td>
            </tr>
            <tr>
              <td>Architecture</td>
              <td>{this.state.jvmState.arch}</td>
            </tr>
            <tr>
              <td>Operating system</td>
              <td>{this.state.jvmState.name}</td>
            </tr>
            <tr>
              <td>Operating system version</td>
              <td>{this.state.jvmState.version}</td>
            </tr>
            <tr>
              <td>Total physical memory</td>
              <td>{this.state.jvmState.totalPhysicalMemory}</td>
            </tr>
            <tr>
              <td>Total cpu load</td>
              <td>{this.state.jvmState.totalCpuLoad}</td>
            </tr>
            <tr>
              <td>Disk size</td>
              <td>{this.state.jvmState.diskSize}</td>
            </tr>
            <tr>
              <td>OS user</td>
              <td>{this.state.jvmState.user}</td>
            </tr>
          </Table>
          <h4>
            <Label bsStyle="danger">Heap</Label>
          </h4>
          <Table striped bordered condensed hover>
            <tr>
              <td>Used</td>
              <td>{this.state.jvmState.heapMemoryUsed}</td>
            </tr>
            <tr>
              <td>Initial</td>
              <td>{this.state.jvmState.heapMemoryInit}</td>
            </tr>
            <tr>
              <td>Committed</td>
              <td>{this.state.jvmState.heapMemoryCommitted}</td>
            </tr>
            <tr>
              <td>Max</td>
              <td>{this.state.jvmState.heapMemoryMax}</td>
            </tr>
          </Table>
          <h4>
            <Label bsStyle="danger">Non-heap</Label>
          </h4>
          <Table striped bordered condensed hover>
            <tr>
              <td>Used</td>
              <td>{this.state.jvmState.nonHeapMemoryUsed}</td>
            </tr>
            <tr>
              <td>Initial</td>
              <td>{this.state.jvmState.nonHeapMemoryInit}</td>
            </tr>
            <tr>
              <td>Committed</td>
              <td>{this.state.jvmState.nonHeapMemoryCommitted}</td>
            </tr>
            <tr>
              <td>Max</td>
              <td>{this.state.jvmState.nonHeapMemoryMax}</td>
            </tr>
          </Table>
          <h4>
            <Label bsStyle="danger">Threads</Label>
          </h4>
          <Table striped bordered condensed hover>
            <tr>
              <td>Current thread count</td>
              <td>{this.state.jvmState.threadCount}</td>
            </tr>
            <tr>
              <td>Total started thread count</td>
              <td>{this.state.jvmState.totalStartedThreadCount}</td>
            </tr>
            <tr>
              <td>Peak thread count</td>
              <td>{this.state.jvmState.peakThreadCount}</td>
            </tr>
          </Table>
        </div>
        <div id="adminIgniteContainer">{igniteMetrics}</div>
      </div>
    );
  }
}
