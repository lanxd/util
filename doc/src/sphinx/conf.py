# -*- coding: utf-8 -*-
#
# Documentation config
#

import sys, os, datetime

sys.path.append(os.path.abspath('utils'))

import sbt_versions

highlight_language = 'scala'
extensions = ['sphinx.ext.extlinks']
templates_path = ['_templates']
source_suffix = '.rst'
master_doc = 'index'

sys.path.append(os.path.abspath('_themes'))
html_theme_path = ['_themes']
html_theme = 'flask'
html_short_title = 'Util'
html_static_path = ['_static']
html_sidebars = {
   'index':    ['sidebarintro.html', 'searchbox.html'],
   '**':       ['sidebarintro.html', 'localtoc.html', 'relations.html', 'searchbox.html']
}
html_favicon = '_static/favicon.ico'
html_theme_options = {
  'index_logo': None
}

project = u'Util'
copyright = u'{} Twitter, Inc'.format(datetime.datetime.now().year)
htmlhelp_basename = "util"
release = sbt_versions.find_release(os.path.abspath('../../../project/Build.scala'))
version = sbt_versions.release_to_version(release)

pygments_style = 'flask_theme_support.FlaskyStyle'

# fall back if theme is not there
try:
    __import__('flask_theme_support')
except ImportError as e:
    print '-' * 74
    print 'Warning: Flask themes unavailable.  Building with default theme'
    print 'If you want the Flask themes, run this command and build again:'
    print
    print '  git submodule update --init'
    print '-' * 74

    pygments_style = 'tango'
    html_theme = 'default'
    html_theme_options = {}
