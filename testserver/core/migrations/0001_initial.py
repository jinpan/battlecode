# -*- coding: utf-8 -*-
from south.utils import datetime_utils as datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):
        # Adding model 'File'
        db.create_table(u'core_file', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('name', self.gf('django.db.models.fields.CharField')(default='RobotPlayer.java', max_length=200, db_index=True)),
            ('content', self.gf('django.db.models.fields.TextField')()),
            ('robot', self.gf('django.db.models.fields.related.ForeignKey')(to=orm['core.Robot'])),
        ))
        db.send_create_signal(u'core', ['File'])

        # Adding model 'Map'
        db.create_table(u'core_map', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('name', self.gf('django.db.models.fields.CharField')(unique=True, max_length=100)),
            ('size', self.gf('django.db.models.fields.IntegerField')(default=35, db_index=True)),
        ))
        db.send_create_signal(u'core', ['Map'])

        # Adding model 'Robot'
        db.create_table(u'core_robot', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('active', self.gf('django.db.models.fields.BooleanField')(default=True, db_index=True)),
            ('create_time', self.gf('django.db.models.fields.DateTimeField')(auto_now_add=True, db_index=True, blank=True)),
            ('creator', self.gf('django.db.models.fields.related.ForeignKey')(to=orm['auth.User'])),
            ('line', self.gf('django.db.models.fields.related.ForeignKey')(to=orm['core.RobotLine'])),
            ('line_num', self.gf('django.db.models.fields.IntegerField')(null=True, db_index=True)),
            ('name', self.gf('django.db.models.fields.CharField')(max_length=100, blank=True)),
        ))
        db.send_create_signal(u'core', ['Robot'])

        # Adding M2M table for field simulated_opponents on 'Robot'
        m2m_table_name = db.shorten_name(u'core_robot_simulated_opponents')
        db.create_table(m2m_table_name, (
            ('id', models.AutoField(verbose_name='ID', primary_key=True, auto_created=True)),
            ('from_robot', models.ForeignKey(orm[u'core.robot'], null=False)),
            ('to_robot', models.ForeignKey(orm[u'core.robot'], null=False))
        ))
        db.create_unique(m2m_table_name, ['from_robot_id', 'to_robot_id'])

        # Adding model 'RobotLine'
        db.create_table(u'core_robotline', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('name', self.gf('django.db.models.fields.CharField')(unique=True, max_length=100)),
            ('alive', self.gf('django.db.models.fields.BooleanField')(default=True, db_index=True)),
        ))
        db.send_create_signal(u'core', ['RobotLine'])

        # Adding model 'Simulation'
        db.create_table(u'core_simulation', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('robot_a', self.gf('django.db.models.fields.related.ForeignKey')(related_name='simulation_a', to=orm['core.Robot'])),
            ('robot_b', self.gf('django.db.models.fields.related.ForeignKey')(blank=True, related_name='simulation_b', null=True, to=orm['core.Robot'])),
            ('map_file', self.gf('django.db.models.fields.related.ForeignKey')(to=orm['core.Map'])),
            ('priority', self.gf('django.db.models.fields.FloatField')(default=0)),
            ('result', self.gf('django.db.models.fields.TextField')(db_index=True, null=True, blank=True)),
            ('error', self.gf('django.db.models.fields.TextField')(null=True, blank=True)),
            ('status', self.gf('django.db.models.fields.CharField')(default='1', max_length=1)),
            ('winner', self.gf('django.db.models.fields.CharField')(max_length=1, null=True, blank=True)),
            ('tie', self.gf('django.db.models.fields.NullBooleanField')(default=None, null=True, db_index=True, blank=True)),
            ('rounds', self.gf('django.db.models.fields.IntegerField')(null=True, blank=True)),
            ('create_time', self.gf('django.db.models.fields.DateTimeField')(auto_now_add=True, db_index=True, blank=True)),
            ('finish_time', self.gf('django.db.models.fields.DateTimeField')(db_index=True, null=True, blank=True)),
            ('name', self.gf('django.db.models.fields.CharField')(max_length=200)),
        ))
        db.send_create_signal(u'core', ['Simulation'])

        # Adding model 'SimulationFile'
        db.create_table(u'core_simulationfile', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('simulation', self.gf('django.db.models.fields.related.OneToOneField')(to=orm['core.Simulation'], unique=True)),
            ('data', self.gf('django.db.models.fields.BinaryField')()),
        ))
        db.send_create_signal(u'core', ['SimulationFile'])

        # Adding model 'SimulationSet'
        db.create_table(u'core_simulationset', (
            (u'id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('name', self.gf('django.db.models.fields.CharField')(max_length=100)),
            ('robot_a', self.gf('django.db.models.fields.related.ForeignKey')(related_name='simulationset_a', to=orm['core.Robot'])),
            ('robot_b', self.gf('django.db.models.fields.related.ForeignKey')(blank=True, related_name='simulationset_b', null=True, to=orm['core.Robot'])),
            ('updated_time', self.gf('django.db.models.fields.DateTimeField')(auto_now=True, blank=True)),
            ('complete', self.gf('django.db.models.fields.BooleanField')(default=False, db_index=True)),
        ))
        db.send_create_signal(u'core', ['SimulationSet'])

        # Adding M2M table for field robot_a_win_simulations on 'SimulationSet'
        m2m_table_name = db.shorten_name(u'core_simulationset_robot_a_win_simulations')
        db.create_table(m2m_table_name, (
            ('id', models.AutoField(verbose_name='ID', primary_key=True, auto_created=True)),
            ('simulationset', models.ForeignKey(orm[u'core.simulationset'], null=False)),
            ('simulation', models.ForeignKey(orm[u'core.simulation'], null=False))
        ))
        db.create_unique(m2m_table_name, ['simulationset_id', 'simulation_id'])

        # Adding M2M table for field robot_a_lose_simulations on 'SimulationSet'
        m2m_table_name = db.shorten_name(u'core_simulationset_robot_a_lose_simulations')
        db.create_table(m2m_table_name, (
            ('id', models.AutoField(verbose_name='ID', primary_key=True, auto_created=True)),
            ('simulationset', models.ForeignKey(orm[u'core.simulationset'], null=False)),
            ('simulation', models.ForeignKey(orm[u'core.simulation'], null=False))
        ))
        db.create_unique(m2m_table_name, ['simulationset_id', 'simulation_id'])

        # Adding M2M table for field robot_a_tie_simulations on 'SimulationSet'
        m2m_table_name = db.shorten_name(u'core_simulationset_robot_a_tie_simulations')
        db.create_table(m2m_table_name, (
            ('id', models.AutoField(verbose_name='ID', primary_key=True, auto_created=True)),
            ('simulationset', models.ForeignKey(orm[u'core.simulationset'], null=False)),
            ('simulation', models.ForeignKey(orm[u'core.simulation'], null=False))
        ))
        db.create_unique(m2m_table_name, ['simulationset_id', 'simulation_id'])


    def backwards(self, orm):
        # Deleting model 'File'
        db.delete_table(u'core_file')

        # Deleting model 'Map'
        db.delete_table(u'core_map')

        # Deleting model 'Robot'
        db.delete_table(u'core_robot')

        # Removing M2M table for field simulated_opponents on 'Robot'
        db.delete_table(db.shorten_name(u'core_robot_simulated_opponents'))

        # Deleting model 'RobotLine'
        db.delete_table(u'core_robotline')

        # Deleting model 'Simulation'
        db.delete_table(u'core_simulation')

        # Deleting model 'SimulationFile'
        db.delete_table(u'core_simulationfile')

        # Deleting model 'SimulationSet'
        db.delete_table(u'core_simulationset')

        # Removing M2M table for field robot_a_win_simulations on 'SimulationSet'
        db.delete_table(db.shorten_name(u'core_simulationset_robot_a_win_simulations'))

        # Removing M2M table for field robot_a_lose_simulations on 'SimulationSet'
        db.delete_table(db.shorten_name(u'core_simulationset_robot_a_lose_simulations'))

        # Removing M2M table for field robot_a_tie_simulations on 'SimulationSet'
        db.delete_table(db.shorten_name(u'core_simulationset_robot_a_tie_simulations'))


    models = {
        u'auth.group': {
            'Meta': {'object_name': 'Group'},
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'name': ('django.db.models.fields.CharField', [], {'unique': 'True', 'max_length': '80'}),
            'permissions': ('django.db.models.fields.related.ManyToManyField', [], {'to': u"orm['auth.Permission']", 'symmetrical': 'False', 'blank': 'True'})
        },
        u'auth.permission': {
            'Meta': {'ordering': "(u'content_type__app_label', u'content_type__model', u'codename')", 'unique_together': "((u'content_type', u'codename'),)", 'object_name': 'Permission'},
            'codename': ('django.db.models.fields.CharField', [], {'max_length': '100'}),
            'content_type': ('django.db.models.fields.related.ForeignKey', [], {'to': u"orm['contenttypes.ContentType']"}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'name': ('django.db.models.fields.CharField', [], {'max_length': '50'})
        },
        u'auth.user': {
            'Meta': {'object_name': 'User'},
            'date_joined': ('django.db.models.fields.DateTimeField', [], {'default': 'datetime.datetime.now'}),
            'email': ('django.db.models.fields.EmailField', [], {'max_length': '75', 'blank': 'True'}),
            'first_name': ('django.db.models.fields.CharField', [], {'max_length': '30', 'blank': 'True'}),
            'groups': ('django.db.models.fields.related.ManyToManyField', [], {'symmetrical': 'False', 'related_name': "u'user_set'", 'blank': 'True', 'to': u"orm['auth.Group']"}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'is_active': ('django.db.models.fields.BooleanField', [], {'default': 'True'}),
            'is_staff': ('django.db.models.fields.BooleanField', [], {'default': 'False'}),
            'is_superuser': ('django.db.models.fields.BooleanField', [], {'default': 'False'}),
            'last_login': ('django.db.models.fields.DateTimeField', [], {'default': 'datetime.datetime.now'}),
            'last_name': ('django.db.models.fields.CharField', [], {'max_length': '30', 'blank': 'True'}),
            'password': ('django.db.models.fields.CharField', [], {'max_length': '128'}),
            'user_permissions': ('django.db.models.fields.related.ManyToManyField', [], {'symmetrical': 'False', 'related_name': "u'user_set'", 'blank': 'True', 'to': u"orm['auth.Permission']"}),
            'username': ('django.db.models.fields.CharField', [], {'unique': 'True', 'max_length': '30'})
        },
        u'contenttypes.contenttype': {
            'Meta': {'ordering': "('name',)", 'unique_together': "(('app_label', 'model'),)", 'object_name': 'ContentType', 'db_table': "'django_content_type'"},
            'app_label': ('django.db.models.fields.CharField', [], {'max_length': '100'}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'model': ('django.db.models.fields.CharField', [], {'max_length': '100'}),
            'name': ('django.db.models.fields.CharField', [], {'max_length': '100'})
        },
        u'core.file': {
            'Meta': {'object_name': 'File'},
            'content': ('django.db.models.fields.TextField', [], {}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'name': ('django.db.models.fields.CharField', [], {'default': "'RobotPlayer.java'", 'max_length': '200', 'db_index': 'True'}),
            'robot': ('django.db.models.fields.related.ForeignKey', [], {'to': u"orm['core.Robot']"})
        },
        u'core.map': {
            'Meta': {'ordering': "['name']", 'object_name': 'Map'},
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'name': ('django.db.models.fields.CharField', [], {'unique': 'True', 'max_length': '100'}),
            'size': ('django.db.models.fields.IntegerField', [], {'default': '35', 'db_index': 'True'})
        },
        u'core.robot': {
            'Meta': {'object_name': 'Robot'},
            'active': ('django.db.models.fields.BooleanField', [], {'default': 'True', 'db_index': 'True'}),
            'create_time': ('django.db.models.fields.DateTimeField', [], {'auto_now_add': 'True', 'db_index': 'True', 'blank': 'True'}),
            'creator': ('django.db.models.fields.related.ForeignKey', [], {'to': u"orm['auth.User']"}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'line': ('django.db.models.fields.related.ForeignKey', [], {'to': u"orm['core.RobotLine']"}),
            'line_num': ('django.db.models.fields.IntegerField', [], {'null': 'True', 'db_index': 'True'}),
            'name': ('django.db.models.fields.CharField', [], {'max_length': '100', 'blank': 'True'}),
            'simulated_opponents': ('django.db.models.fields.related.ManyToManyField', [], {'to': u"orm['core.Robot']", 'symmetrical': 'False'})
        },
        u'core.robotline': {
            'Meta': {'object_name': 'RobotLine'},
            'alive': ('django.db.models.fields.BooleanField', [], {'default': 'True', 'db_index': 'True'}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'name': ('django.db.models.fields.CharField', [], {'unique': 'True', 'max_length': '100'})
        },
        u'core.simulation': {
            'Meta': {'ordering': "['map_file__name']", 'object_name': 'Simulation'},
            'create_time': ('django.db.models.fields.DateTimeField', [], {'auto_now_add': 'True', 'db_index': 'True', 'blank': 'True'}),
            'error': ('django.db.models.fields.TextField', [], {'null': 'True', 'blank': 'True'}),
            'finish_time': ('django.db.models.fields.DateTimeField', [], {'db_index': 'True', 'null': 'True', 'blank': 'True'}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'map_file': ('django.db.models.fields.related.ForeignKey', [], {'to': u"orm['core.Map']"}),
            'name': ('django.db.models.fields.CharField', [], {'max_length': '200'}),
            'priority': ('django.db.models.fields.FloatField', [], {'default': '0'}),
            'result': ('django.db.models.fields.TextField', [], {'db_index': 'True', 'null': 'True', 'blank': 'True'}),
            'robot_a': ('django.db.models.fields.related.ForeignKey', [], {'related_name': "'simulation_a'", 'to': u"orm['core.Robot']"}),
            'robot_b': ('django.db.models.fields.related.ForeignKey', [], {'blank': 'True', 'related_name': "'simulation_b'", 'null': 'True', 'to': u"orm['core.Robot']"}),
            'rounds': ('django.db.models.fields.IntegerField', [], {'null': 'True', 'blank': 'True'}),
            'status': ('django.db.models.fields.CharField', [], {'default': "'1'", 'max_length': '1'}),
            'tie': ('django.db.models.fields.NullBooleanField', [], {'default': 'None', 'null': 'True', 'db_index': 'True', 'blank': 'True'}),
            'winner': ('django.db.models.fields.CharField', [], {'max_length': '1', 'null': 'True', 'blank': 'True'})
        },
        u'core.simulationfile': {
            'Meta': {'object_name': 'SimulationFile'},
            'data': ('django.db.models.fields.BinaryField', [], {}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'simulation': ('django.db.models.fields.related.OneToOneField', [], {'to': u"orm['core.Simulation']", 'unique': 'True'})
        },
        u'core.simulationset': {
            'Meta': {'object_name': 'SimulationSet'},
            'complete': ('django.db.models.fields.BooleanField', [], {'default': 'False', 'db_index': 'True'}),
            u'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'name': ('django.db.models.fields.CharField', [], {'max_length': '100'}),
            'robot_a': ('django.db.models.fields.related.ForeignKey', [], {'related_name': "'simulationset_a'", 'to': u"orm['core.Robot']"}),
            'robot_a_lose_simulations': ('django.db.models.fields.related.ManyToManyField', [], {'related_name': "'robot_a_lose_simulations'", 'symmetrical': 'False', 'to': u"orm['core.Simulation']"}),
            'robot_a_tie_simulations': ('django.db.models.fields.related.ManyToManyField', [], {'related_name': "'robot_a_tie_simulations'", 'symmetrical': 'False', 'to': u"orm['core.Simulation']"}),
            'robot_a_win_simulations': ('django.db.models.fields.related.ManyToManyField', [], {'related_name': "'robot_a_win_simulations'", 'symmetrical': 'False', 'to': u"orm['core.Simulation']"}),
            'robot_b': ('django.db.models.fields.related.ForeignKey', [], {'blank': 'True', 'related_name': "'simulationset_b'", 'null': 'True', 'to': u"orm['core.Robot']"}),
            'updated_time': ('django.db.models.fields.DateTimeField', [], {'auto_now': 'True', 'blank': 'True'})
        }
    }

    complete_apps = ['core']