/*
 * This file is part of WebLookAndFeel library.
 *
 * WebLookAndFeel library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WebLookAndFeel library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WebLookAndFeel library.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.alee.managers.style.data;

import com.alee.laf.Styles;
import com.alee.managers.style.StyleException;
import com.alee.managers.style.SupportedComponent;
import com.alee.utils.CompareUtils;
import com.alee.utils.ReflectUtils;
import com.alee.utils.TextUtils;
import com.alee.utils.xml.InsetsConverter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/**
 * Custom XStream converter for ComponentStyle class.
 *
 * @author Mikle Garin
 * @see <a href="https://github.com/mgarin/weblaf/wiki/How-to-use-StyleManager">How to use StyleManager</a>
 * @see com.alee.managers.style.StyleManager
 * @see com.alee.managers.style.data.ComponentStyle
 */

public class ComponentStyleConverter extends ReflectionConverter
{
    /**
     * Converter constants.
     */
    public static final String STYLE_NODE = "style";
    public static final String COMPONENT_TYPE_ATTRIBUTE = "type";
    public static final String STYLE_ID_ATTRIBUTE = "id";
    public static final String EXTENDS_ID_ATTRIBUTE = "extends";
    public static final String MARGIN_ATTRIBUTE = "margin";
    public static final String PADDING_ATTRIBUTE = "padding";
    public static final String COMPONENT_NODE = "component";
    public static final String COMPONENT_CLASS_ATTRIBUTE = "class";
    public static final String UI_NODE = "ui";
    public static final String PAINTER_NODE = "painter";
    public static final String PAINTER_ID_ATTRIBUTE = "id";
    public static final String PAINTER_IDS_SEPARATOR = ",";
    public static final String DEFAULT_PAINTER_ID = "painter";
    public static final String PAINTER_CLASS_ATTRIBUTE = "class";
    public static final String IGNORED_ATTRIBUTE = "ignored";

    /**
     * Constructs ComponentStyleConverter with the specified mapper and reflection provider.
     *
     * @param mapper             mapper
     * @param reflectionProvider reflection provider
     */
    public ComponentStyleConverter ( final Mapper mapper, final ReflectionProvider reflectionProvider )
    {
        super ( mapper, reflectionProvider );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvert ( final Class type )
    {
        return type.equals ( ComponentStyle.class );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void marshal ( final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context )
    {
        final ComponentStyle componentStyle = ( ComponentStyle ) source;
        final Map<String, Object> componentProperties = componentStyle.getComponentProperties ();
        final Map<String, Object> uiProperties = componentStyle.getUIProperties ();
        final List<PainterStyle> painters = componentStyle.getPainters ();

        // Style component type
        writer.addAttribute ( COMPONENT_TYPE_ATTRIBUTE, componentStyle.getType ().toString () );

        // Component style ID
        final String componentStyleId = componentStyle.getId ();
        writer.addAttribute ( STYLE_ID_ATTRIBUTE, componentStyleId != null ? componentStyleId : Styles.defaultStyle );

        // Extended style ID
        final String extendsId = componentStyle.getExtendsId ();
        if ( extendsId != null )
        {
            writer.addAttribute ( EXTENDS_ID_ATTRIBUTE, extendsId );
        }

        // Margin and padding
        if ( uiProperties != null )
        {
            final Insets margin = ( Insets ) uiProperties.get ( MARGIN_ATTRIBUTE );
            if ( margin != null )
            {
                writer.addAttribute ( MARGIN_ATTRIBUTE, InsetsConverter.insetsToString ( margin ) );
            }
            final Insets padding = ( Insets ) uiProperties.get ( PADDING_ATTRIBUTE );
            if ( padding != null )
            {
                writer.addAttribute ( PADDING_ATTRIBUTE, InsetsConverter.insetsToString ( padding ) );
            }
        }

        // Component properties
        if ( componentProperties != null )
        {
            writer.startNode ( COMPONENT_NODE );
            for ( final Map.Entry<String, Object> property : componentProperties.entrySet () )
            {
                writer.startNode ( property.getKey () );
                context.convertAnother ( property.getValue () );
                writer.endNode ();
            }
            writer.endNode ();
        }

        // UI properties
        if ( uiProperties != null )
        {
            writer.startNode ( UI_NODE );
            for ( final Map.Entry<String, Object> property : uiProperties.entrySet () )
            {
                final String key = property.getKey ();
                if ( !CompareUtils.equals ( key, MARGIN_ATTRIBUTE, PADDING_ATTRIBUTE ) )
                {
                    writer.startNode ( key );
                    context.convertAnother ( property.getValue () );
                    writer.endNode ();
                }
            }
            writer.endNode ();
        }

        // Painters
        if ( painters != null )
        {
            for ( final PainterStyle painterStyle : painters )
            {
                writer.startNode ( PAINTER_NODE );
                if ( !CompareUtils.equals ( painterStyle.getId (), DEFAULT_PAINTER_ID ) )
                {
                    writer.addAttribute ( PAINTER_ID_ATTRIBUTE, painterStyle.getId () );
                }
                writer.addAttribute ( PAINTER_CLASS_ATTRIBUTE, painterStyle.getPainterClass () );
                for ( final Map.Entry<String, Object> property : painterStyle.getProperties ().entrySet () )
                {
                    writer.startNode ( property.getKey () );
                    context.convertAnother ( property.getValue () );
                    writer.endNode ();
                }
                writer.endNode ();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object unmarshal ( final HierarchicalStreamReader reader, final UnmarshallingContext context )
    {
        // Creating component style
        final ComponentStyle componentStyle = new ComponentStyle ();
        final Map<String, Object> componentProperties = new LinkedHashMap<String, Object> ();
        final Map<String, Object> uiProperties = new LinkedHashMap<String, Object> ();
        final List<PainterStyle> painters = new ArrayList<PainterStyle> ();

        // Reading style component type
        final SupportedComponent type = SupportedComponent.valueOf ( reader.getAttribute ( COMPONENT_TYPE_ATTRIBUTE ) );
        componentStyle.setType ( type );

        // Reading style ID
        final String componentStyleId = reader.getAttribute ( STYLE_ID_ATTRIBUTE );
        componentStyle.setId ( componentStyleId != null ? componentStyleId : Styles.defaultStyle );

        // Reading extended style ID
        componentStyle.setExtendsId ( reader.getAttribute ( EXTENDS_ID_ATTRIBUTE ) );

        // Reading margin and padding
        final String margin = reader.getAttribute ( MARGIN_ATTRIBUTE );
        if ( margin != null )
        {
            uiProperties.put ( MARGIN_ATTRIBUTE, InsetsConverter.insetsFromString ( margin ) );
        }
        final String padding = reader.getAttribute ( PADDING_ATTRIBUTE );
        if ( padding != null )
        {
            uiProperties.put ( PADDING_ATTRIBUTE, InsetsConverter.insetsFromString ( padding ) );
        }

        // Reading component style properties and painters
        // Using LinkedHashMap to keep properties order intact
        while ( reader.hasMoreChildren () )
        {
            // Read next node
            reader.moveDown ();
            final String nodeName = reader.getNodeName ();
            if ( nodeName.equals ( COMPONENT_NODE ) )
            {
                // Reading component property
                final String componentClassName = reader.getAttribute ( COMPONENT_CLASS_ATTRIBUTE );
                final Class<? extends JComponent> cc = ReflectUtils.getClassSafely ( componentClassName );
                final Class<? extends JComponent> typeClass = type.getComponentClass ();
                if ( cc != null && !typeClass.isAssignableFrom ( cc ) )
                {
                    throw new StyleException ( "Specified custom component class \"" + cc.getCanonicalName () +
                            "\" is not assignable from the base component class \"" + typeClass.getCanonicalName () + "\"" );
                }
                final Class<? extends JComponent> componentClass = cc != null ? cc : typeClass;
                while ( reader.hasMoreChildren () )
                {
                    reader.moveDown ();
                    final String propName = reader.getNodeName ();
                    readProperty ( reader, context, componentStyleId, componentProperties, componentClass, propName );
                    reader.moveUp ();
                }
            }
            else if ( nodeName.equals ( UI_NODE ) )
            {
                // Reading UI property
                final Class uiClass = type.getUIClass ();
                while ( reader.hasMoreChildren () )
                {
                    reader.moveDown ();
                    final String propName = reader.getNodeName ();
                    readProperty ( reader, context, componentStyleId, uiProperties, uiClass, propName );
                    reader.moveUp ();
                }
            }
            else if ( nodeName.equals ( PAINTER_NODE ) )
            {
                // Collecting style IDs
                final String ids = reader.getAttribute ( PAINTER_ID_ATTRIBUTE );
                final boolean emptyIds = TextUtils.isEmpty ( ids );
                final List<String> indices = new ArrayList<String> ( 1 );
                if ( !emptyIds && ids.contains ( PAINTER_IDS_SEPARATOR ) )
                {
                    final StringTokenizer st = new StringTokenizer ( ids, PAINTER_IDS_SEPARATOR, false );
                    while ( st.hasMoreTokens () )
                    {
                        final String id = st.nextToken ();
                        indices.add ( TextUtils.isEmpty ( id ) ? DEFAULT_PAINTER_ID : id );
                    }
                }
                else
                {
                    indices.add ( emptyIds ? DEFAULT_PAINTER_ID : ids );
                }

                // Creating separate painter styles for each style ID
                // This might be the case when the same style scheme applied to more than one painter
                final String painterClassName = reader.getAttribute ( PAINTER_CLASS_ATTRIBUTE );
                final List<PainterStyle> separateStyles = new ArrayList<PainterStyle> ( indices.size () );
                for ( final String id : indices )
                {
                    final PainterStyle painterStyle = new PainterStyle ();
                    painterStyle.setId ( id );
                    painterStyle.setPainterClass ( painterClassName );
                    separateStyles.add ( painterStyle );
                }
                context.put ( PAINTER_CLASS_ATTRIBUTE, painterClassName );

                // Reading painter style properties
                // Using LinkedHashMap to keep properties order
                final Map<String, Object> painterProperties = new LinkedHashMap<String, Object> ();
                final Class painterClass = ReflectUtils.getClassSafely ( painterClassName );
                if ( painterClass != null )
                {
                    while ( reader.hasMoreChildren () )
                    {
                        reader.moveDown ();
                        final String propName = reader.getNodeName ();
                        readProperty ( reader, context, componentStyleId, painterProperties, painterClass, propName );
                        reader.moveUp ();
                    }
                }
                for ( final PainterStyle painterStyle : separateStyles )
                {
                    painterStyle.setProperties ( painterProperties );
                }

                // Adding read painter style
                painters.addAll ( separateStyles );
            }
            reader.moveUp ();
        }

        // Marking base painter
        if ( componentStyle.getExtendsId () == null )
        {
            if ( painters.size () == 1 )
            {
                painters.get ( 0 ).setBase ( true );
            }
            else
            {
                boolean baseSet = false;
                for ( final PainterStyle painter : painters )
                {
                    if ( painter.isBase () )
                    {
                        baseSet = true;
                        break;
                    }
                }
                if ( !baseSet && painters.size () > 0 )
                {
                    painters.get ( 0 ).setBase ( true );
                }
            }
        }

        // Updating values we have just read
        componentStyle.setComponentProperties ( componentProperties );
        componentStyle.setUIProperties ( uiProperties );
        componentStyle.setPainters ( painters );

        return componentStyle;
    }

    /**
     * Parses single style property into properties map.
     *
     * @param reader           hierarchical stream reader
     * @param context          unmarshalling context
     * @param componentStyleId component style ID
     * @param properties       properties
     * @param propertyClass    property class
     * @param propertyName     property name
     */
    protected void readProperty ( final HierarchicalStreamReader reader, final UnmarshallingContext context, final String componentStyleId,
                                  final Map<String, Object> properties, final Class propertyClass, final String propertyName )
    {
        final String ignored = reader.getAttribute ( IGNORED_ATTRIBUTE );
        if ( ignored != null && Boolean.parseBoolean ( ignored ) )
        {
            properties.put ( propertyName, IgnoredValue.VALUE );
        }
        else
        {
            final Class fieldClass = ReflectUtils.getFieldTypeSafely ( propertyClass, propertyName );
            if ( fieldClass != null )
            {
                properties.put ( propertyName, context.convertAnother ( properties, fieldClass ) );
            }
            else
            {
                final Method getter = ReflectUtils.getFieldGetter ( propertyClass, propertyName );
                if ( getter != null )
                {
                    final Class<?> rClass = getter.getReturnType ();
                    properties.put ( propertyName, context.convertAnother ( properties, rClass ) );
                }
                else
                {
                    throw new StyleException ( "Component property \"" + propertyName + "\" type from style \"" + componentStyleId +
                            "\" cannot be determined! Make sure it points to existing field or getter method" );
                }
            }
        }
    }
}