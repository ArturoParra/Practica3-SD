import { useEffect, useState } from 'react'
import './index.css'
import MonsterCard from './components/MonsterCard'

function App() {

  const [socket, setSocket] = useState(null);
  const [gamePhase, setGamePhase] = useState('connecting'); // 'connecting', 'choosing', 'fighting'
  const [monsterChoices, setMonsterChoices] = useState([]);
  const [gameMessage, setGameMessage] = useState('');

  const [playerMonster, setPlayerMonster] = useState(null)

  const [opponentMonster, setOpponentMonster] = useState(null)

  const [playerTurn, setPlayerTurn] = useState();

  useEffect(() => {
    const newSocket = new WebSocket('ws://localhost:8080');

    newSocket.onopen = () => {
      console.log('Connected to WebSocket server');
      setSocket(newSocket);
    };

    newSocket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      console.log('Message from server: ', message);

      // Un switch es bueno para manejar diferentes tipos de mensajes
      switch (message.type) {
        case 'GAME_STATE_UPDATE':
          // El servidor nos da el estado completo y personalizado
          setPlayerMonster(message.yourMonster);
          setOpponentMonster(message.opponentMonster);
          setPlayerTurn(message.isYourTurn);
          setGamePhase('fighting');

          // Manejar el fin del juego
          if (message.isGameOver) {
            alert(`Game Over! Winner: ${message.winner}`);
            setGamePhase('connecting');
            setGameMessage('Connecting to server...');
          }
          break;

        case 'WAITING_FOR_OPPONENT':
          setGamePhase('waiting');
          setGameMessage('Waiting for an opponent to join...');
          break;

        case 'CHOOSE_MONSTER':
          setPlayerMonster(null);
          setOpponentMonster(null);
          setPlayerTurn(false);
          setGamePhase('choosing');
          setMonsterChoices(message.choices);
          setGameMessage('Choose your monster!');
          break;

        case 'GAME_START': // This can now be merged into GAME_STATE_UPDATE
          setGamePhase('fighting');
          setGameMessage('The battle begins!');
          break;

        default:
          console.warn('Unknown message type:', message.type);
          break;

        // ... otros casos como 'OPPONENT_DISCONNECTED'
      }
    };

    return () => {
      newSocket.close();
    }

  }, [])

  const handlePlayerAction = (action) => {
    if (socket && playerTurn) {
      const message = {
        type: 'PLAYER_ACTION',
        payload: { name: action.name }
      }
      const jsonmessage = JSON.stringify(message);
      socket.send(jsonmessage);
    }
  }

  const handleSelectMonster = (monsterName) => {
    if (socket) {
      const message = {
        type: 'SELECT_MONSTER',
        payload: monsterName // El payload ahora es correctamente un string
      };
      socket.send(JSON.stringify(message));
      setGameMessage('Waiting for opponent to choose...');
    }
  };

  return (
    <>
      {gamePhase === 'connecting' && (
        <div className="text-center mt-10">
          <h2 className="text-2xl">Connecting to server...</h2>
        </div>
      )}

      {gamePhase === 'waiting' && (
        <div className="text-center mt-10">
          <h2 className="text-2xl">{gameMessage}</h2>
        </div>
      )}

      {gamePhase === 'choosing' && (
        <div className="text-center mt-10">
          <h2 className="text-2xl mb-4">{gameMessage}</h2>
          <div className="flex flex-col md:flex-row justify-center items-center p-2">
            {monsterChoices.map((monster, index) => (
              <div key={index} className="flex flex-col justify-center items-center border p-4 rounded w-1/2 md:w-1/4 m-2">
                <h3 className="text-xl font-bold">{monster.name}</h3>
                <img src={monster.imageUrl} alt={monster.name} className="h-64 object-contain mb-2 rounded-md shadow-2xl" />
                <p>HP: {monster.hp}</p>
                <button
                  className="mt-2 bg-blue-500 px-4 py-2 rounded"
                  onClick={() => {
                    setPlayerMonster(monster);
                    setGamePhase('fighting');
                    setGameMessage('You chose ' + monster.name + '! Waiting for opponent...');
                    handleSelectMonster(monster.name);
                  }}
                >
                  Choose
                </button>
              </div>
            ))}
          </div >
        </div >
      )
      }

      {gamePhase === 'fighting' && !opponentMonster && (
        <div className="text-center mt-10">
          <h2 className="text-2xl">{gameMessage}</h2>
        </div>
      )}

      {
        gamePhase === 'fighting' && opponentMonster && (
          <div className="text-center mt-10">
            <div className="flex justify-around">
              <div className="flex w-full items-center flex-col text-center">
                <div>YOU</div>
                <MonsterCard monster={playerMonster} />
              </div>

              <div className="flex w-full items-center flex-col text-center">
                <div>ENEMY</div>
                <MonsterCard monster={opponentMonster} />
              </div>

            </div>

            {playerTurn ? (
              <div className="mt-6">
                <h3 className="text-xl mb-2">Your Actions:</h3>
                <div className="flex justify-center space-x-4">
                  {playerMonster.actions.map((action, index) => (
                    <button
                      key={index}
                      className="bg-blue-500 px-4 py-2 rounded"
                      onClick={() => handlePlayerAction(action)}
                    >
                      {action.name}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="mt-6">
                <h3 className="text-xl mb-2">Opponent's Turn:</h3>
                <p>Waiting for opponent to act...</p>
              </div>
            )}
          </div>
        )
      }

    </>
  )
}

export default App
