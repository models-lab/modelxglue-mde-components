require 'java'
require 'json'

DEBUG=false

def get_mar_root()
    if File.exists?('/.dockerenv')
        return DEBUG ? '/shared' : '/mar-jesus-artefact'
    else
       ENV["REPO_MAR"] || (raise "REPO_MAR not set")
    end
end

indexer_jar = File.join(get_mar_root(), "mar-indexer-spark/target/mar-indexer-spark-1.0-SNAPSHOT-jar-with-dependencies.jar")
require indexer_jar

scorer_jar = File.join(get_mar_root(), "mar-restservice/target/mar.restservice-1.0-SNAPSHOT-jar-with-dependencies.jar")
require scorer_jar


class ClassificationIndexer < Java::MarSqlite::SqliteIndexer
    def initialize(ids_to_models)
        super()
        self.withIncludeMetada(true)
        @ids_to_models = ids_to_models
    end

    def loadModel(origin)
        id = origin.getModelId()
        content = @ids_to_models[id]
        resource = Java::MarAnalysisEcore::EcoreLoader.new().toEMF(content)
        return Java::MarSparkIndexer::LoadedModel.new(resource, origin)
    end
end

def read_configuration(filename)
    # IndexJobConfigurationData
    conf = Java::MarIndexerCommonCmd::CmdOptions.readConfiguration(java.io.File.new(filename))
    return conf.getRepo("repo-experiment-ecore")
end

def main(root, mode)
    # Register XMI factory to be able to load Ecore files
    Java::OrgEclipseEmfEcoreResource::Resource::Factory::Registry::INSTANCE.getExtensionToFactoryMap( ).put("*",
        Java::OrgEclipseEmfEcoreXmiImpl::XMIResourceFactoryImpl.new());

    job = read_configuration(File.join(File.dirname(__FILE__), "config.json"))

    # job = Java::MarIndexerCommonConfiguration::SingleIndexJob.new
    db_file = File.join(root, "index.db")

    if mode == 'train' and File.exist?(db_file)
        File.delete(db_file)
    end

    index_db = Java::MarSqlite::SqliteIndexDatabase.new(java.io.File.new(db_file))

    # Set hyper-parameters
    params = JSON.parse(File.read(File.join(root, "hyper.json")))
    job.setGraphLength(params["hyper"]["length"])

    x_d = JSON.parse(File.read(File.join(root, "X.json")))
    if mode == 'train'
        y_d = JSON.parse(File.read(File.join(root, "y.json")))

        ids_to_models = {}
        origins = []
        x_d.each_with_index { |m, idx|
            id = m["ids"]
            label = y_d[idx].to_s

            origin = Java::MarSparkIndexer::ModelOrigin.new(id, id, job, label)

            ids_to_models[id] = m["xmi"]
            origins.push(origin)
        }

        ClassificationIndexer.new(ids_to_models).index(index_db, origins)
    elsif mode == 'predict'
        path_computation = job.toPathComputation()
        sqlite_scorer = Java::MarRestserviceScoring::SqliteScorer.new(path_computation, index_db)

        k_parameter = 3
        y_pred = []
        x_d.each_with_index { |m, idx|
            content = m["xmi"]
            resource = Java::MarAnalysisEcore::EcoreLoader.new().toEMF(content)
            result = sqlite_scorer.sortedScore(resource);

            # Implement the KNN
            best = {}
            idx = 0
            it = result.entrySet().iterator()
            while it.hasNext()
                idx += 1
                item = it.next()
                puts "Finding with #{item.getKey()}"
                label = index_db.getModelById(item.getKey())
                best[label] = (best[label] || 0) + 1 
                puts "Best label #{label} with #{best[label]}"
                if idx = k_parameter
                    break
                end
            end

            winner = best.sort_by { |k, v| v }.reverse[0][0]
            y_pred.push(winner)
        }

        # Transform all y_pred to integer if all string values has an integer format
        # This is to support integer labels
        if y_pred.all? { |x| x.to_i.to_s == x }
            y_pred = y_pred.map { |x| x.to_i }
        end

        # Write the result y_pred as json file
        File.open(File.join(root, "y_pred.json"), 'w') { |f|
            f.write(JSON.pretty_generate(y_pred))
        }
    else
        raise "Unknown mode #{mode}"
    end

    index_db.close()
end

if __FILE__ == $0
    root = ARGV[0]
    task = ARGV[1]
    mode = ARGV[2]
    main(root, mode)
end

