/*
 * Copyright 2014 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package flow.path;

import android.view.View;
import android.view.ViewGroup;
import flow.Flow;
import flow.ViewState;

/**
 * Handles swapping paths within a container view, as well as flow mechanics, allowing supported
 * container views to be largely declarative.
 */
public abstract class PathContainer {

  private static final ViewState NULL_VIEW_STATE = new NullViewState();

  /**
   * Provides information about the current or most recent Traversal handled by the container.
   */
  protected static final class TraversalState {
    private Path fromPath;
    private ViewState fromViewState;
    private Path toPath;
    private ViewState toViewState;

    public void setNextEntry(Path path, ViewState viewState) {
      this.fromPath = this.toPath;
      this.fromViewState = this.toViewState;
      this.toPath = path;
      this.toViewState = viewState;
    }

    public Path fromPath() {
      return fromPath;
    }

    public Path toPath() {
      return toPath;
    }

    public void saveViewState(View view) {
      fromViewState.save(view);
    }

    public void restoreViewState(View view) {
      toViewState.restore(view);
    }
  }

  private final int tagKey;

  protected PathContainer(int tagKey) {
    this.tagKey = tagKey;
  }

  public final void executeTraversal(PathContainerView view, Flow.Traversal traversal,
      final Flow.TraversalCallback callback) {
    ViewState viewState = traversal.destination.currentViewState();
    doShowPath(view, traversal.destination.<Path>top(), traversal.direction, viewState, callback);
  }

  /**
   * Replace the current view and show the given path. Allows display of {@link Path}s other
   * than in response to Flow dispatches.
   */
  public void setPath(PathContainerView view, Path path, Flow.Direction direction,
      Flow.TraversalCallback callback) {
    doShowPath(view, path, direction, NULL_VIEW_STATE, callback);
  }

  private void doShowPath(PathContainerView view, Path path, Flow.Direction direction,
      ViewState viewState, Flow.TraversalCallback callback) {
    final View oldChild = view.getCurrentChild();
    Path oldPath;
    ViewGroup containerView = view.getContainerView();
    TraversalState traversalState = ensureTag(containerView);

    // See if we already have the direct child we want, and if so short circuit the traversal.
    if (oldChild != null) {
      oldPath = Preconditions.checkNotNull(traversalState.toPath,
          "Container view has child %s with no path", oldChild.toString());
      if (oldPath.equals(path)) {
        callback.onTraversalCompleted();
        return;
      }
    }

    traversalState.setNextEntry(path, viewState);
    performTraversal(containerView, traversalState, direction, callback);
  }

  protected abstract void performTraversal(ViewGroup container, TraversalState traversalState,
      Flow.Direction direction, Flow.TraversalCallback callback);

  private TraversalState ensureTag(ViewGroup container) {
    TraversalState traversalState = (TraversalState) container.getTag(tagKey);
    if (traversalState == null) {
      traversalState = new TraversalState();
      container.setTag(tagKey, traversalState);
    }
    return traversalState;
  }

  private static final class NullViewState implements ViewState {
    @Override public void save(View view) {
    }

    @Override public void restore(View view) {
    }
  }
}
