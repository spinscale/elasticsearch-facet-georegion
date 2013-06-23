/*
 * Copyright 2012 Igor Motov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsearch.plugin.search.facet.georegion;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.plugin.service.georegion.GeoRegionService;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.search.facet.FacetModule;
import org.elasticsearch.search.facet.georegion.GeoRegionFacetParser;

import java.util.Collection;

public class GeoRegionFacetPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "facet-georegion";
    }

    @Override
    public String description() {
        return "Georegion facet support";
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        services.add(GeoRegionService.class);
        return services;
    }

    public void onModule(FacetModule facetModule) {
        facetModule.addFacetProcessor(GeoRegionFacetParser.class);
    }

}
