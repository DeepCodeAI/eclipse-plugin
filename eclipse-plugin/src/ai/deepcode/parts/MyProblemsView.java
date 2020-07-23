package ai.deepcode.parts;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;
import org.eclipse.ui.internal.views.markers.CachedMarkerBuilder;
import org.eclipse.ui.internal.views.markers.ExtendedMarkersView;
import org.eclipse.ui.internal.views.markers.MarkerContentGenerator;
import org.eclipse.ui.internal.views.markers.MarkerTypeFieldFilter;
import org.eclipse.ui.internal.views.markers.ProblemsView;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.markers.MarkerFieldFilter;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.eclipse.ui.views.markers.internal.ContentGeneratorDescriptor;
import org.eclipse.ui.views.markers.internal.MarkerMessages;
import org.eclipse.ui.views.markers.internal.MarkerSupportRegistry;
import org.eclipse.ui.views.markers.internal.MarkerType;
import ai.deepcode.core.DCLogger;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Listener;

@SuppressWarnings("restriction")
public class MyProblemsView extends ProblemsView /* MarkerSupportView */ {
  // public MyProblemsView() {
  // super(MarkerSupportRegistry.PROBLEMS_GENERATOR);
  // }

  // private static final Image image_logo =
  // AbstractUIPlugin.imageDescriptorFromPlugin("ai.deepcode", "icons/logo.png").createImage();
  // @Override
  // void updateTitleImage(Integer[] counts) {
  // setTitleImage(image_logo);
  // }

  @Override
  public void init(IViewSite site, IMemento m) throws PartInitException {
    super.init(site, m);

    // Hack to set Filter to show only deepCode markers. No official APIs to do that.
    MarkerContentGenerator generator = null;
    try {
      Field fieldGenerator = ExtendedMarkersView.class.getDeclaredField("generator");
      fieldGenerator.setAccessible(true);
      generator = (MarkerContentGenerator) fieldGenerator.get(this); // getGenerator(this);
      if (generator == null) {
        DCLogger.getInstance().logWarn("MarkerContentGenerator is NULL at " + this);
        return;
      }

      // creating our Filter from scratch.
      // class MarkerFieldFilterGroup is not public (and actually loaded with another classloader)
      Class<?> classMarkerFieldFilterGroup =
          this.getClass().getClassLoader().loadClass("org.eclipse.ui.internal.views.markers.MarkerFieldFilterGroup");
      Constructor<?> con =
          classMarkerFieldFilterGroup.getDeclaredConstructor(org.eclipse.core.runtime.IConfigurationElement.class,
              org.eclipse.ui.internal.views.markers.MarkerContentGenerator.class);
      con.setAccessible(true);
      Object filter = con.newInstance(null, generator);

      Field fieldName = classMarkerFieldFilterGroup.getDeclaredField("name");
      fieldName.setAccessible(true);
      fieldName.set(filter, "DeepCodeMarkersFilter");

      // to fill "filters" field at generator
      Method writeFiltersPreference = generator.getClass().getDeclaredMethod("writeFiltersPreference");
      writeFiltersPreference.setAccessible(true);
      writeFiltersPreference.invoke(generator);

      Field filtersField = MarkerContentGenerator.class.getDeclaredField("filters");
      filtersField.setAccessible(true);
      @SuppressWarnings("rawtypes")
      Collection filtersCollection = (Collection) filtersField.get(generator);
      // remove existing filters at "generator" and add our Filter
      filtersCollection.clear();
      filtersCollection.add(filter);

      // to fill "fieldFilters" field at new filter (MarkerFieldFilterGroup)
      writeFiltersPreference.invoke(generator);

      // getting field with MarkerTypeFieldFilter responsible for filtering by marker type
      Field fieldFilters = classMarkerFieldFilterGroup.getDeclaredField("fieldFilters");
      fieldFilters.setAccessible(true);
      MarkerFieldFilter[] MarkerFieldFilters = (MarkerFieldFilter[]) fieldFilters.get(filter);
      MarkerTypeFieldFilter markerTypeFieldFilter = null;
      for (MarkerFieldFilter markerFieldFilter : MarkerFieldFilters) {
        if (markerFieldFilter instanceof MarkerTypeFieldFilter) {
          markerTypeFieldFilter = (MarkerTypeFieldFilter) markerFieldFilter;
        }
      }
      if (markerTypeFieldFilter == null)
        throw new NoSuchFieldException("No MarkerTypeFieldFilter at: " + MarkerFieldFilters);
      
      // actually set to show only DeepCode type markers
      Field selectedTypes = MarkerTypeFieldFilter.class.getDeclaredField("selectedTypes");
      selectedTypes.setAccessible(true);
      @SuppressWarnings("unchecked")
      Collection<MarkerType> selectedTypesCollection = (Collection<MarkerType>) selectedTypes.get(markerTypeFieldFilter);
      
      selectedTypesCollection.removeIf(markerType -> {return !markerType.getId().equals("ai.deepcode.deepcodemarker");});

    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
        | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException e) {
      DCLogger.getInstance().logWarn("Exception during Filter initialisation: " + e.getMessage() + e.getStackTrace());
    }
  }

  // @Override
  // void updateTitleImage(Integer[] counts) {
  // Image image=
  // WorkbenchPlugin.getDefault().getSharedImages().getImage(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW);
  // if (counts[0].intValue() > 0) {
  // image=
  // WorkbenchPlugin.getDefault().getSharedImages().getImage(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_ERROR);
  // } else if (counts[1].intValue() > 0) {
  // image=
  // WorkbenchPlugin.getDefault().getSharedImages().getImage(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_WARNING);
  // } else if (counts[2].intValue() > 0) {
  // image=
  // WorkbenchPlugin.getDefault().getSharedImages().getImage(IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_INFO);
  // }
  // setTitleImage(image);
  // }

  // @Override
  // protected IUndoContext getUndoContext() {
  // return WorkspaceUndoUtil.getProblemsUndoContext();
  // }
  //
  // @Override
  // protected String getDeleteOperationName(IMarker[] markers) {
  // Assert.isLegal(markers.length > 0);
  // return markers.length == 1 ? MarkerMessages.deleteProblemMarker_operationName :
  // MarkerMessages.deleteProblemMarkers_operationName;
  // }

}
