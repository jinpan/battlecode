bc_template = (
'''# Match server settings
bc.server.throttle=yield
bc.server.throttle-count=50

# Game engine settings
bc.engine.debug-methods=false
bc.engine.silence-a=false
bc.engine.silence-b=false
bc.engine.gc=false
bc.engine.gc-rounds=50
bc.engine.upkeep=true
bc.engine.breakpoints=false
bc.engine.bytecodes-used=true

# Client settings
bc.client.opengl=false
bc.client.use-models=true
bc.client.renderprefs2d=
bc.client.renderprefs3d=
bc.client.sound-on=true
bc.client.check-updates=true
bc.client.viewer-delay=50

# Headless settings - for "ant file"
bc.game.team-a=%s
bc.game.team-b=%s
bc.game.maps=%s
bc.server.save-file=match.rms

# Transcriber settings
bc.server.transcribe-input=matches\\match.rms
bc.server.transcribe-output=matches\\transcribed.txt
''')

build_template = (
'''
<project name="Battlecode 2014" basedir="." default="run">

  <description>
    Build file for Battlecode 2014 players.
  </description>

  <property name="path.base" location="."/>
  <property name="path.lib" location="${path.base}/lib"/>

  <property name="path.teams" location="${path.base}/teams"/>
  <property name="path.maps" location="${path.base}/maps"/>
  <property name="path.bin" location="${path.base}/bin"/>
  <property name="path.matches" location="${path.base}/matches"/>
  
  <fileset id="files.build" dir="${path.lib}">
    <include name="*.jar"/>
  </fileset>

  <fileset id="scala.files" dir="${path.lib}" includes="scala-*.jar"/>
  <pathconvert property="scala" refid="scala.files" setonempty="false"/>
  
  <path id="classpath.run">
    <dirset dir="${path.bin}"/>
    <dirset dir="${path.lib}"/>
    <dirset dir="${path.teams}"/>
    <fileset refid="files.build"/>
  </path>
  
  <target name="-init">
    <mkdir dir="${path.bin}"/>
  </target>

  <target name="clean">
    <delete dir="${path.bin}"/>
  </target>

  <target name="build-scala" if="scala">
	<taskdef resource="scala/tools/ant/antlib.xml">
		<classpath>
			<fileset dir="${path.lib}">
				<include name="scala-library-2.9.1.jar"/>
				<include name="scala-compiler-2.9.1.jar"/>
			</fileset>
		</classpath>
	</taskdef>
		
	<scalac
		srcdir="${path.teams}"
    	destdir="${path.bin}"
    	target="jvm-1.5"
		classpathref="classpath.run">
		<include name="**/*.scala"/>
		<include name="**/*.java"/>
  	</scalac>
  </target>

  <target name="build" depends="-init,build-scala">
    <javac 
     classpathref="classpath.run" 
     destdir="${path.bin}" 
     srcdir="${path.teams}"
	 target="1.6"
	 source="1.6"
     debug="true">
      <compilerarg line="-Xlint"/>
    </javac>
  </target>

  <target name="run" depends="build">
    <java 
     classpathref="classpath.run"
     fork="yes"
     classname="battlecode.client.Main">
      <jvmarg value="-Dapple.awt.graphics.UseQuartz=true"/>
      <jvmarg value="-Dbc.server.map-path=${path.maps}"/>
      <jvmarg value="-Xmx256m"/> 
	  <jvmarg value="-Djava.library.path=${path.lib}" />
      <arg line="-c %s"/>
    </java>
  </target> 

  <target name="runmatch" depends="build">
    <java 
     classpathref="classpath.run"
     fork="yes"
     classname="battlecode.client.Main">
      <jvmarg value="-Dapple.awt.graphics.UseQuartz=true"/>
      <jvmarg value="-Dbc.server.map-path=${path.maps}"/>
      <jvmarg value="-Dbc.client.match=${match}"/>
      <jvmarg value="-Xmx256m"/> 
	  <jvmarg value="-Djava.library.path=${path.lib}" />
      <arg line="-c %s"/>
    </java>
  </target> 

  <target name="serve" depends="build">
    <java
     classpathref="classpath.run"
     fork="yes"
     classname="battlecode.server.Main">
      <jvmarg value="-Dbc.server.mode=tcp"/>
      <arg line="-c %s"/>
    </java>
  </target>

  <target name="file" depends="build">
    <java
     classpathref="classpath.run"
     fork="yes"
     classname="battlecode.server.Main">
      <jvmarg value="-Dbc.server.mode=headless"/>
      <arg line="-c %s"/>
    </java>
  </target>

  <target name="jar" depends="build">
    <fail unless="team">
    run as "ant -Dteam=name jar" where "name" is a folder in your teams folder.
    </fail>
    <jar destfile="${path.base}/submission.jar"
      basedir="${path.teams}"
      includes="${team}/**/*.java,${team}/**/*.scala"/>
  </target>
</project>
''')

